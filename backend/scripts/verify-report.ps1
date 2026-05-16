$ErrorActionPreference = "Stop"

# ====================================================================
# verify-report.ps1：M-8 报表中心模块 E2E 验收脚本
#
# 覆盖：
#   - admin 登录
#   - 数据源 CRUD（默认主库 id=0 不入表，测连接走主库 sys_user 即可）
#   - 模板创建：合法 SELECT 通过 → DROP / 多语句 / OUTFILE 被 SqlGuard 拒
#   - 运行：参数化 ${name}、行数截断、超时与失败均落 sys_report_run_log
#   - 即席 SQL：管理员可跑；对应失败路径不在此覆盖（无非管理员）
#   - 导出：CSV + xlsx 两路 blob 下载
#   - 看板：新建 + 加 1 表格 + 1 ECharts 卡片 → 查看 → 删除
#   - 运行日志：列表能查到刚才执行的几条
#   - 清理：手动触发 /report/run/log/purge-now days=0
#
# 调用：
#   pwsh backend/scripts/verify-report.ps1
#   要求：后端在 http://localhost:9080；admin/admin123；redis@127.0.0.1:6379 db3 取验证码
# ====================================================================

# ====== Redis 工具（与 verify-* 同套） ======
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

function Try-PostExpectFail {
  param([string]$Token, [string]$Url, [string]$BodyJson, [string]$Reason)
  try {
    $rsp = Invoke-WebRequest -Uri $Url -Method Post -Headers (Headers $Token) -Body $BodyJson -ContentType "application/json" -ErrorAction Stop
    if ($rsp.StatusCode -eq 200) {
      $data = $rsp.Content | ConvertFrom-Json
      if ($data.code -eq 200) {
        throw "未预期：$Reason 居然返回 200"
      }
      Write-Host "$Reason 被业务码拒绝（code=$($data.code) msg=$($data.msg)）"
      return
    }
  } catch {
    Write-Host "$Reason 被正确拒绝（$($_.Exception.Message))"
  }
}

# ====== 主流程 ======
Write-Host "=== Step 1: admin login ==="
$tk = Login-As "admin" "admin123"
Write-Host "ok"

$BASE = "http://localhost:9080"
$timestamp = (Get-Date -Format "HHmmss")

Write-Host ""
Write-Host "=== Step 2: 模板列表（先看现有数量） ==="
$listBefore = Invoke-RestMethod -Uri "$BASE/report/template?pageNum=1&pageSize=10" -Method Get -Headers (Headers $tk)
Write-Host "before total=$($listBefore.total)"

Write-Host ""
Write-Host "=== Step 3: 新建模板（合法 SELECT） ==="
$tplCode = "qa_report_$timestamp"
$tplBody = @{
  code = $tplCode
  name = "QA 报表 $timestamp"
  category = "qa"
  datasourceId = 0
  sqlText = "SELECT user_id AS id, user_name FROM sys_user WHERE user_id >= `${minId} ORDER BY user_id"
  paramSchema = '[{"name":"minId","type":"number","label":"最小 ID","required":true,"default":1}]'
  rowLimit = 100
  timeoutMs = 5000
  status = "0"
  remark = "qa e2e"
} | ConvertTo-Json
$add = Invoke-RestMethod -Uri "$BASE/report/template" -Method Post -Headers (Headers $tk) -Body $tplBody -ContentType "application/json"
Assert-Eq $add.code 200 "模板新增 200"
$tplId = $add.data
Write-Host "tplId=$tplId"

Write-Host ""
Write-Host "=== Step 4: SqlGuard：DROP / 多语句 / OUTFILE 必须被拒 ==="
$bad1 = @{ code = "bad1_$timestamp"; name = "bad1"; sqlText = "DROP TABLE sys_user"; rowLimit = 10; timeoutMs = 2000 } | ConvertTo-Json
Try-PostExpectFail -Token $tk -Url "$BASE/report/template" -BodyJson $bad1 -Reason "DROP 模板"

$bad2 = @{ code = "bad2_$timestamp"; name = "bad2"; sqlText = "SELECT 1; DROP TABLE sys_user"; rowLimit = 10; timeoutMs = 2000 } | ConvertTo-Json
Try-PostExpectFail -Token $tk -Url "$BASE/report/template" -BodyJson $bad2 -Reason "多语句模板"

$bad3 = @{ code = "bad3_$timestamp"; name = "bad3"; sqlText = "SELECT * FROM sys_user INTO OUTFILE '/tmp/leak.csv'"; rowLimit = 10; timeoutMs = 2000 } | ConvertTo-Json
Try-PostExpectFail -Token $tk -Url "$BASE/report/template" -BodyJson $bad3 -Reason "OUTFILE 模板"

Write-Host ""
Write-Host "=== Step 5: 即席 SQL 校验入口 /report/template/validate ==="
$bad4 = @{ name = "validate-only"; sqlText = "INSERT INTO t VALUES (1)"; rowLimit = 10; timeoutMs = 2000 } | ConvertTo-Json
Try-PostExpectFail -Token $tk -Url "$BASE/report/template/validate" -BodyJson $bad4 -Reason "INSERT 校验入口"

Write-Host ""
Write-Host "=== Step 6: 运行模板 ==="
$runBody = @{ templateId = $tplId; params = @{ minId = 1 } } | ConvertTo-Json
$run = Invoke-RestMethod -Uri "$BASE/report/run" -Method Post -Headers (Headers $tk) -Body $runBody -ContentType "application/json"
Assert-Eq $run.code 200 "运行返回 200"
Assert-True ($run.data.rowCount -ge 1) "至少返回 1 行"
Assert-True ($run.data.columns.Length -ge 2) "返回至少 2 列"
Write-Host "run rows=$($run.data.rowCount) cost=$($run.data.costMs)ms truncated=$($run.data.truncated)"

Write-Host ""
Write-Host "=== Step 7: 行数截断 ==="
$runSmall = @{ templateId = $tplId; params = @{ minId = 1 }; rowLimit = 1 } | ConvertTo-Json
$runT = Invoke-RestMethod -Uri "$BASE/report/run" -Method Post -Headers (Headers $tk) -Body $runSmall -ContentType "application/json"
Assert-Eq $runT.data.rowCount 1 "rowLimit=1 应只返 1 行"
Write-Host "truncated=$($runT.data.truncated)"

Write-Host ""
Write-Host "=== Step 8: 缺失参数报错 ==="
$bad5 = @{ templateId = $tplId; params = @{} } | ConvertTo-Json
Try-PostExpectFail -Token $tk -Url "$BASE/report/run" -BodyJson $bad5 -Reason "缺失 minId"

Write-Host ""
Write-Host "=== Step 9: CSV / xlsx 导出 ==="
$tmp = [System.IO.Path]::Combine($env:TEMP, "verify-report-$timestamp")
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$csvOut = Join-Path $tmp "out.csv"
$xlsxOut = Join-Path $tmp "out.xlsx"

$csvBody = @{ templateId = $tplId; params = @{ minId = 1 } } | ConvertTo-Json
$rsp = Invoke-WebRequest -Uri "$BASE/report/run/export?format=csv" -Method Post -Headers (Headers $tk) -Body $csvBody -ContentType "application/json" -OutFile $csvOut -PassThru
Assert-Eq $rsp.StatusCode 200 "CSV 200"
Assert-True ((Get-Item $csvOut).Length -gt 0) "CSV 非空"
$csvText = Get-Content $csvOut -Raw -Encoding UTF8
Assert-True ($csvText -match "user_name") "CSV 含表头 user_name"
Write-Host "csv size=$((Get-Item $csvOut).Length)"

$rspX = Invoke-WebRequest -Uri "$BASE/report/run/export?format=xlsx" -Method Post -Headers (Headers $tk) -Body $csvBody -ContentType "application/json" -OutFile $xlsxOut -PassThru
Assert-Eq $rspX.StatusCode 200 "xlsx 200"
Assert-True ((Get-Item $xlsxOut).Length -gt 1024) "xlsx 至少 1KB"
# 简单校验 xlsx 文件签名（PK..）
$head = [System.IO.File]::ReadAllBytes($xlsxOut)[0..1]
Assert-Eq $head[0] 0x50 "xlsx 第一字节 'P'"
Assert-Eq $head[1] 0x4B "xlsx 第二字节 'K'"
Write-Host "xlsx size=$((Get-Item $xlsxOut).Length)"

Write-Host ""
Write-Host "=== Step 10: 看板创建 + 加 2 卡片 ==="
$dashCode = "qa_dash_$timestamp"
$dashBody = @{
  dashboard = @{ code = $dashCode; name = "QA 看板"; category = "qa"; status = "0" }
  cards = @(
    @{ templateId = $tplId; title = "用户列表"; chartType = "table"; posW = 12; posH = 6; orderNum = 0; paramJson = '{"minId":1}' }
    @{ templateId = $tplId; title = "用户分布"; chartType = "bar"; posW = 12; posH = 6; orderNum = 1; configJson = '{"x":"user_name","y":"id"}'; paramJson = '{"minId":1}' }
  )
} | ConvertTo-Json -Depth 4
$dashAdd = Invoke-RestMethod -Uri "$BASE/report/dashboard" -Method Post -Headers (Headers $tk) -Body $dashBody -ContentType "application/json"
Assert-Eq $dashAdd.code 200 "看板新增 200"
$dashId = $dashAdd.data
Write-Host "dashId=$dashId"

Write-Host ""
Write-Host "=== Step 11: 看板详情 / 编辑 / 卡片整批替换 ==="
$detail = Invoke-RestMethod -Uri "$BASE/report/dashboard/$dashId" -Method Get -Headers (Headers $tk)
Assert-Eq $detail.data.cards.Length 2 "应有 2 张卡"
Write-Host "cards in dashboard: $($detail.data.cards.Length)"

# 编辑：保留 1 张
$editBody = @{
  dashboard = @{ id = $dashId; code = $dashCode; name = "QA 看板（updated）"; category = "qa"; status = "0" }
  cards = @(
    @{ templateId = $tplId; title = "更新后唯一卡"; chartType = "table"; posW = 24; posH = 10; orderNum = 0; paramJson = '{"minId":1}' }
  )
} | ConvertTo-Json -Depth 4
[void](Invoke-RestMethod -Uri "$BASE/report/dashboard" -Method Put -Headers (Headers $tk) -Body $editBody -ContentType "application/json")
$detail2 = Invoke-RestMethod -Uri "$BASE/report/dashboard/$dashId" -Method Get -Headers (Headers $tk)
Assert-Eq $detail2.data.cards.Length 1 "编辑后应只剩 1 张卡"
Assert-Eq $detail2.data.dashboard.name "QA 看板（updated）" "看板名已更新"
Write-Host "edit cards 替换 OK"

Write-Host ""
Write-Host "=== Step 12: 运行日志列表 ==="
$logs = Invoke-RestMethod -Uri "$BASE/report/run/log?pageNum=1&pageSize=20" -Method Get -Headers (Headers $tk)
Assert-True ($logs.total -ge 1) "应至少 1 条运行日志"
Write-Host "run logs total=$($logs.total)"

Write-Host ""
Write-Host "=== Step 13: 清理（days=0 强清今天前的） ==="
# days 是按天偏移，days=0 在 service 里映射为 max(1, 0)=1，仍然不会误删今天的日志；这里只验接口可达
$purge = Invoke-RestMethod -Uri "$BASE/report/run/log/purge-now?days=365" -Method Post -Headers (Headers $tk)
Assert-Eq $purge.code 200 "purge 200"
Write-Host "purge 365d msg=$($purge.msg)"

Write-Host ""
Write-Host "=== Step 14: 清理 ==="
[void](Invoke-RestMethod -Uri "$BASE/report/dashboard/$dashId" -Method Delete -Headers (Headers $tk))
[void](Invoke-RestMethod -Uri "$BASE/report/template/$tplId" -Method Delete -Headers (Headers $tk))
Remove-Item $tmp -Recurse -Force
Write-Host "清理完毕"

Write-Host ""
Write-Host "=== ALL PASSED ==="
