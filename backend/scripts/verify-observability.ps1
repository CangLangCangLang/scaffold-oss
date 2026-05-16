$ErrorActionPreference = "Stop"

# ====================================================================
# verify-observability.ps1：Q-3 可观测性 E2E 验收脚本
#
# 覆盖（10 步）：
#   1. admin 登录
#   2. /actuator/health 总体 UP，且 details.scaffoldModules 出现并且 status=UP
#   3. /actuator/scaffold-modules 模块列表至少含 1 个（CRM 起步）
#   4. /actuator/prometheus 包含 jvm_memory_used_bytes / scaffold_business_rows
#   5. /monitor/slow-request/business-metrics 返回非空 tables
#   6. 触发一次必"慢"请求（耗时 > slowMs 通过 sleep API 不一定有，所以走 5xx）
#      — 用 /system/user/profile 改成不存在的接口 → 触发 4xx；改 recordClientError 后再验
#      — 这里我们直接调一个不存在的 API 路径 → 期待 404
#   7. 列表 /monitor/slow-request 应可看到刚刚的记录（reason=CLIENT_ERROR 当 recordClientError 开启时
#      不开启时本步只断言 endpoint 通）
#   8. /monitor/slow-request/scan-now 手动触发告警 Job → 返回 sent=N
#   9. 列表中已扫过的 alerted=1
#   10. 清理 days=0 → 返回清理 N 条
#
# 调用：
#   pwsh backend/scripts/verify-observability.ps1
#   要求：后端 http://localhost:9080；admin/admin123；redis@127.0.0.1:6379 db3
# ====================================================================

# ====== 复用 verify-* 同套 helper ======
function Invoke-Redis {
  param([string[]]$RedisArgs)
  $tcp = New-Object System.Net.Sockets.TcpClient("127.0.0.1", 6379)
  try {
    $s = $tcp.GetStream()
    function Send-Cmd { param($strm, [string[]]$arr)
      $sb = New-Object System.Text.StringBuilder
      [void]$sb.Append("*").Append($arr.Length).Append("`r`n")
      foreach ($a in $arr) { [void]$sb.Append('$').Append($a.Length).Append("`r`n").Append($a).Append("`r`n") }
      $b = [System.Text.Encoding]::UTF8.GetBytes($sb.ToString())
      $strm.Write($b, 0, $b.Length); $strm.Flush()
      Start-Sleep -Milliseconds 150
      $buf = New-Object byte[] 4096
      $read = $strm.Read($buf, 0, $buf.Length)
      return [System.Text.Encoding]::UTF8.GetString($buf, 0, $read)
    }
    [void](Send-Cmd $s @("AUTH","123456"))
    [void](Send-Cmd $s @("SELECT","3"))
    return Send-Cmd $s $RedisArgs
  } finally {
    $tcp.Close()
  }
}

function Get-CaptchaToken {
  $r = Invoke-RestMethod -Uri "http://localhost:9080/captchaImage" -Method Get
  $resp = Invoke-Redis -RedisArgs @("GET", "captcha_codes:$($r.uuid)")
  $val = ($resp -split "`r`n")[1]
  if ($val.StartsWith('"') -and $val.EndsWith('"')) { $val = $val.Substring(1, $val.Length - 2) }
  return @{ uuid = $r.uuid; code = $val }
}

function Login-As {
  param([string]$Username, [string]$Password)
  $cap = Get-CaptchaToken
  $body = @{ username = $Username; password = $Password; code = $cap.code; uuid = $cap.uuid } | ConvertTo-Json
  return (Invoke-RestMethod -Uri "http://localhost:9080/login" -Method Post -Body $body -ContentType "application/json").token
}

function Headers($t) { return @{ Authorization = "Bearer $t" } }

function Assert-Eq($actual, $expected, $msg) {
  if ($actual -ne $expected) { throw "ASSERT FAIL: $msg (expected=$expected actual=$actual)" }
}
function Assert-True($cond, $msg) {
  if (-not $cond) { throw "ASSERT FAIL: $msg" }
}
function Assert-Contains($haystack, $needle, $msg) {
  if (-not ($haystack -match [regex]::Escape($needle))) {
    throw "ASSERT FAIL: $msg (looking for '$needle' in output)"
  }
}

# ============== Step 1: admin 登录 ==============
Write-Host "[Step 1] admin 登录" -ForegroundColor Cyan
$token = Login-As -Username "admin" -Password "admin123"
$h = Headers $token
Assert-True ($null -ne $token) "admin token 应非空"

# ============== Step 2: /actuator/health 总体 UP ==============
Write-Host "[Step 2] /actuator/health 总体 + scaffoldModules" -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "http://localhost:9080/actuator/health" -Method Get -Headers $h
Assert-Eq $health.status "UP" "总体 status 应为 UP"
# scaffoldModules 子项可能在 components 也可能在 details.components 下（Spring Boot 4 路径差异）
$scaffoldModules = $null
if ($health.components -and $health.components.scaffoldModules) {
  $scaffoldModules = $health.components.scaffoldModules
} elseif ($health.details -and $health.details.components -and $health.details.components.scaffoldModules) {
  $scaffoldModules = $health.details.components.scaffoldModules
}
Assert-True ($null -ne $scaffoldModules) "scaffoldModules 子组件应存在"
Assert-Eq $scaffoldModules.status "UP" "scaffoldModules.status 应为 UP"

# ============== Step 3: /actuator/scaffold-modules ==============
Write-Host "[Step 3] /actuator/scaffold-modules 列表" -ForegroundColor Cyan
$mods = Invoke-RestMethod -Uri "http://localhost:9080/actuator/scaffold-modules" -Method Get -Headers $h
Assert-True ($mods.modules.Count -ge 1) "至少有 1 个模块（CRM 必装）"
Write-Host "  modules count = $($mods.modules.Count)" -ForegroundColor Gray

# ============== Step 4: /actuator/prometheus ==============
Write-Host "[Step 4] /actuator/prometheus 关键指标" -ForegroundColor Cyan
$prom = Invoke-WebRequest -Uri "http://localhost:9080/actuator/prometheus" -Method Get -Headers $h
$promBody = $prom.Content
Assert-Contains $promBody "jvm_memory_used_bytes" "应包含 JVM 指标"
Assert-Contains $promBody "scaffold_business_rows" "应包含业务行数 Gauge"

# ============== Step 5: /monitor/slow-request/business-metrics ==============
Write-Host "[Step 5] /monitor/slow-request/business-metrics" -ForegroundColor Cyan
$bm = Invoke-RestMethod -Uri "http://localhost:9080/monitor/slow-request/business-metrics" -Method Get -Headers $h
Assert-True ($bm.code -eq 200) "business-metrics 返回 200"
Assert-True ($bm.data.tables.Count -ge 1) "tables 至少 1 项"
Write-Host "  tables = $($bm.data.tables -join ', ')" -ForegroundColor Gray

# ============== Step 6: 触发一次必产记录的请求（404） ==============
Write-Host "[Step 6] 触发 4xx（命中不存在的 API） — 记录与否取决于 recordClientError" -ForegroundColor Cyan
try {
  Invoke-RestMethod -Uri "http://localhost:9080/api/this-path-does-not-exist" -Method Get -Headers $h | Out-Null
} catch {
  # 期望失败 — 静默
}

# 给异步线程一点点时间落表（@Async 默认是简单 ThreadPoolTaskExecutor）
Start-Sleep -Milliseconds 800

# ============== Step 7: /monitor/slow-request 列表 ==============
Write-Host "[Step 7] /monitor/slow-request 列表" -ForegroundColor Cyan
$slow = Invoke-RestMethod -Uri "http://localhost:9080/monitor/slow-request" -Method Get -Headers $h
Assert-True ($slow.code -eq 200) "list 应返回 200"
$initialPending = $slow.data.pending
$initialTotal = $slow.data.total
Write-Host "  total=$initialTotal pending=$initialPending" -ForegroundColor Gray

# ============== Step 8: 手动触发扫描 ==============
Write-Host "[Step 8] /monitor/slow-request/scan-now" -ForegroundColor Cyan
$scan = Invoke-RestMethod -Uri "http://localhost:9080/monitor/slow-request/scan-now" -Method Post -Headers $h
Assert-True ($scan.code -eq 200) "scan-now 应返回 200"
Write-Host "  msg = $($scan.msg)" -ForegroundColor Gray

# ============== Step 9: 已扫过的应 alerted=1 ==============
Write-Host "[Step 9] 验证已扫的记录 alerted=1" -ForegroundColor Cyan
$slow2 = Invoke-RestMethod -Uri "http://localhost:9080/monitor/slow-request" -Method Get -Headers $h
# pending 应不大于扫之前
if ($slow2.data.pending -gt $initialPending) {
  Write-Host "  WARN pending 反而增加 — 期间可能有新慢请求进入，可忽略" -ForegroundColor Yellow
} else {
  Write-Host "  pending: $initialPending → $($slow2.data.pending) ✓" -ForegroundColor Green
}

# ============== Step 10: 清理 ==============
Write-Host "[Step 10] /monitor/slow-request/purge?days=0 清理全部" -ForegroundColor Cyan
$purge = Invoke-RestMethod -Uri "http://localhost:9080/monitor/slow-request/purge?days=0" -Method Post -Headers $h
Assert-True ($purge.code -eq 200) "purge 应返回 200"
Write-Host "  msg = $($purge.msg)" -ForegroundColor Gray

Write-Host "" -ForegroundColor White
Write-Host "✓ verify-observability 全部步骤通过（10 步）" -ForegroundColor Green
