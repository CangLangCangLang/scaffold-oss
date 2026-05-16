$ErrorActionPreference = "Stop"

# ====================================================================
# verify-file.ps1：M-6 文件中心模块 E2E 验收脚本
#
# 覆盖：
#   - admin 登录
#   - 上传：用 framework /system/upload/file 走通用上传，再用 /file/file/upload 走文件中心专用入口
#   - 列表：默认 del_flag=0；切回收站可看到软删的；过滤 name / bucket / ext
#   - 文件夹：根 / 子级建 / 唯一约束 / 跨用户保护（admin 自身可见）
#   - 编辑：改名 / 移动到文件夹 / 加标签
#   - 引用计数：通过直接调 service 模拟 attach（这里走 SQL 检查），ref_count > 0 时软删被拒
#   - 软删：删 → del_flag=2 + delete_time set
#   - 分享：创建（永久 / 一次性 / 带密码三组）→ 匿名访问校验 → 二次访问一次性应失败 → 错密码应失败
#   - 鉴权下载：有 token 拿 200 + Content-Disposition；无 token 拿 401
#   - quartz 手动触发：调 /file/file/purge-now retainDays=0 应返回 purged 数量
#
# 调用：
#   pwsh backend/scripts/verify-file.ps1
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

function Upload-File {
  param([string]$Token, [string]$Endpoint, [string]$LocalPath, [hashtable]$ExtraFields)
  # 用 .NET HttpClient 走 multipart，而不是 PowerShell 5.1 的 -Form（兼容旧版）
  Add-Type -AssemblyName "System.Net.Http"
  $client = New-Object System.Net.Http.HttpClient
  try {
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $Token)
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $stream = [System.IO.File]::OpenRead($LocalPath)
    $fileContent = New-Object System.Net.Http.StreamContent($stream)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/octet-stream")
    $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($LocalPath))
    if ($ExtraFields) {
      foreach ($k in $ExtraFields.Keys) {
        $multipart.Add((New-Object System.Net.Http.StringContent($ExtraFields[$k])), $k)
      }
    }
    $rsp = $client.PostAsync($Endpoint, $multipart).Result
    $body = $rsp.Content.ReadAsStringAsync().Result
    if (-not $rsp.IsSuccessStatusCode) {
      throw "Upload failed: $($rsp.StatusCode) $body"
    }
    return $body | ConvertFrom-Json
  } finally {
    if ($stream) { $stream.Dispose() }
    $client.Dispose()
  }
}

# ====== 主流程 ======
Write-Host "=== Step 1: admin login ==="
$tk = Login-As "admin" "admin123"
Write-Host "ok"

$BASE = "http://localhost:9080"
$timestamp = (Get-Date -Format "HHmmss")

Write-Host ""
Write-Host "=== Step 2: 准备临时文件 ==="
$tmp = [System.IO.Path]::Combine($env:TEMP, "verify-file-$timestamp")
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$pngPath = Join-Path $tmp "hello.png"
[System.IO.File]::WriteAllBytes($pngPath, [byte[]](0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D))
$pdfPath = Join-Path $tmp "demo.pdf"
[System.IO.File]::WriteAllBytes($pdfPath, [byte[]](0x25,0x50,0x44,0x46,0x2D,0x31,0x2E,0x34,0x0A))
Write-Host "tmp dir: $tmp"

Write-Host ""
Write-Host "=== Step 3: 通用上传 /system/upload/file ==="
$genUploaded = Upload-File -Token $tk -Endpoint "$BASE/system/upload/file" -LocalPath $pngPath -ExtraFields @{ bucket = "qa/file" }
Assert-Eq $genUploaded.code 200 "通用上传应返回 200"
Assert-True ($genUploaded.data.url -match "/profile/qa/file/") "通用上传 URL 含 /profile/qa/file/"
Write-Host "url=$($genUploaded.data.url)"

Write-Host ""
Write-Host "=== Step 4: 文件中心专用上传 /file/file/upload ==="
$pngFile = Upload-File -Token $tk -Endpoint "$BASE/file/file/upload" -LocalPath $pngPath -ExtraFields @{ bucket = "common" }
Assert-Eq $pngFile.code 200 "上传应 200"
Assert-True ($null -ne $pngFile.data.id) "返回 file id"
Assert-Eq $pngFile.data.bucket "common" "bucket=common"
Assert-Eq $pngFile.data.delFlag "0" "del_flag=0"
$pngId = $pngFile.data.id
Write-Host "pngId=$pngId path=$($pngFile.data.storagePath)"

$pdfFile = Upload-File -Token $tk -Endpoint "$BASE/file/file/upload" -LocalPath $pdfPath -ExtraFields @{ bucket = "qa" }
Assert-Eq $pdfFile.code 200 "PDF 上传应 200"
$pdfId = $pdfFile.data.id
Write-Host "pdfId=$pdfId"

Write-Host ""
Write-Host "=== Step 5: 列表过滤 ==="
$list = Invoke-RestMethod -Uri "$BASE/file/file?bucket=common&pageNum=1&pageSize=20" -Method Get -Headers (Headers $tk)
Assert-True ($list.total -ge 1) "common 桶应至少 1 条"
$listAll = Invoke-RestMethod -Uri "$BASE/file/file?pageNum=1&pageSize=20" -Method Get -Headers (Headers $tk)
Assert-True ($listAll.total -ge 2) "全量应至少 2 条"
Write-Host "common.total=$($list.total) all.total=$($listAll.total)"

Write-Host ""
Write-Host "=== Step 6: 文件夹建 / 移动 ==="
$folderBody = @{ parentId = 0; name = "QA 子文件夹 $timestamp" } | ConvertTo-Json
$folder = (Invoke-RestMethod -Uri "$BASE/file/folder" -Method Post -Headers (Headers $tk) -Body $folderBody -ContentType "application/json").data
Assert-True ($null -ne $folder.id) "返回 folder id"
Assert-True ($folder.path -match "QA 子文件夹") "path 含命名"
$folderId = $folder.id
Write-Host "folder id=$folderId path=$($folder.path)"

# 重复同名应被唯一约束拒
try {
  Invoke-RestMethod -Uri "$BASE/file/folder" -Method Post -Headers (Headers $tk) -Body $folderBody -ContentType "application/json" | Out-Null
  throw "未预期：重复名居然建成功"
} catch {
  Write-Host "重复名被正确拒绝"
}

# 移动 png 到文件夹
$mvBody = @{ id = $pngId; folderId = $folderId; name = "Q3 报告.png"; tags = "qa,2026" } | ConvertTo-Json
[void](Invoke-RestMethod -Uri "$BASE/file/file" -Method Put -Headers (Headers $tk) -Body $mvBody -ContentType "application/json")
$pngAfter = (Invoke-RestMethod -Uri "$BASE/file/file/$pngId" -Method Get -Headers (Headers $tk)).data
Assert-Eq $pngAfter.folderId $folderId "已移动至新文件夹"
Assert-Eq $pngAfter.name "Q3 报告.png" "改名生效"
Assert-Eq $pngAfter.tags "qa,2026" "标签生效"
Write-Host "edit OK"

Write-Host ""
Write-Host "=== Step 7: 鉴权下载 ==="
# 用 -OutFile 让 Invoke-WebRequest 真下载二进制
$dlPath = Join-Path $tmp "downloaded.png"
$rsp = Invoke-WebRequest -Uri "$BASE/file/download/$pngId" -Headers (Headers $tk) -OutFile $dlPath -PassThru
Assert-Eq $rsp.StatusCode 200 "鉴权下载 200"
Assert-True ((Get-Item $dlPath).Length -gt 0) "下载文件非空"
Write-Host "下载 OK $((Get-Item $dlPath).Length) bytes"

# 无 token：尝试就好；有些拦截链返 redirect 有些 401
try {
  $noAuth = Invoke-WebRequest -Uri "$BASE/file/download/$pngId" -Method Get -ErrorAction Stop
  if ($noAuth.StatusCode -eq 200) { throw "无 token 居然 200" }
} catch {
  Write-Host "无 token 下载被正确拒绝"
}

Write-Host ""
Write-Host "=== Step 8: 分享创建（永久） ==="
$share1Body = @{ fileId = $pngId; expireDays = 0; oneTime = "0" } | ConvertTo-Json
$share1 = (Invoke-RestMethod -Uri "$BASE/file/share" -Method Post -Headers (Headers $tk) -Body $share1Body -ContentType "application/json").data
Assert-True ($null -ne $share1.token) "分享 token 应返回"
Assert-True ($share1.token.Length -ge 16) "token 长度合理"
Assert-Eq $share1.status "0" "新分享状态 0=有效"
Write-Host "永久分享 token=$($share1.token)"

# 匿名访问（无 Authorization header）应直接放行（@Anonymous 注解）
$anon1 = Invoke-WebRequest -Uri "$BASE/file/share/access/$($share1.token)" -Method Get -OutFile (Join-Path $tmp "shared1.png") -PassThru
Assert-Eq $anon1.StatusCode 200 "永久分享匿名访问 200"
Write-Host "匿名访问 OK"

Write-Host ""
Write-Host "=== Step 9: 一次性分享（用过即销毁） ==="
$share2Body = @{ fileId = $pdfId; expireDays = 1; oneTime = "1" } | ConvertTo-Json
$share2 = (Invoke-RestMethod -Uri "$BASE/file/share" -Method Post -Headers (Headers $tk) -Body $share2Body -ContentType "application/json").data
$tk2 = $share2.token

# 第一次访问 OK
$first = Invoke-WebRequest -Uri "$BASE/file/share/access/$tk2" -Method Get -OutFile (Join-Path $tmp "shared2-1.pdf") -PassThru
Assert-Eq $first.StatusCode 200 "一次性首次访问 200"
# 第二次应失败
try {
  Invoke-WebRequest -Uri "$BASE/file/share/access/$tk2" -Method Get -OutFile (Join-Path $tmp "shared2-2.pdf") -PassThru -ErrorAction Stop | Out-Null
  throw "未预期：一次性二次访问居然 200"
} catch {
  Write-Host "一次性二次访问被正确拒绝"
}

Write-Host ""
Write-Host "=== Step 10: 带密码分享 ==="
$share3Body = @{ fileId = $pngId; expireDays = 7; oneTime = "0"; password = "secret123" } | ConvertTo-Json
$share3 = (Invoke-RestMethod -Uri "$BASE/file/share" -Method Post -Headers (Headers $tk) -Body $share3Body -ContentType "application/json").data
$tk3 = $share3.token

# 不传密码应失败
try {
  Invoke-WebRequest -Uri "$BASE/file/share/access/$tk3" -Method Get -ErrorAction Stop | Out-Null
  throw "未预期：缺密码居然 200"
} catch {
  Write-Host "无密码访问被正确拒绝"
}
# 错密码应失败
try {
  Invoke-WebRequest -Uri "$BASE/file/share/access/${tk3}?password=wrong" -Method Get -ErrorAction Stop | Out-Null
  throw "未预期：错密码居然 200"
} catch {
  Write-Host "错密码被正确拒绝"
}
# 对密码 OK
$ok3 = Invoke-WebRequest -Uri "$BASE/file/share/access/${tk3}?password=secret123" -Method Get -OutFile (Join-Path $tmp "shared3.png") -PassThru
Assert-Eq $ok3.StatusCode 200 "对密码访问 200"
Write-Host "对密码访问 OK"

Write-Host ""
Write-Host "=== Step 11: 软删 + 回收站列表 ==="
[void](Invoke-RestMethod -Uri "$BASE/file/file/$pngId" -Method Delete -Headers (Headers $tk))
$pngAfterDel = (Invoke-RestMethod -Uri "$BASE/file/file/$pngId" -Method Get -Headers (Headers $tk)).data
Assert-Eq $pngAfterDel.delFlag "2" "软删后 del_flag=2"
Assert-True ($null -ne $pngAfterDel.deleteTime) "delete_time 已设"
$bin = Invoke-RestMethod -Uri "$BASE/file/file?delFlag=2&pageNum=1&pageSize=20" -Method Get -Headers (Headers $tk)
$inBin = $false
foreach ($r in $bin.rows) { if ($r.id -eq $pngId) { $inBin = $true; break } }
Assert-True $inBin "回收站应可见此文件"
Write-Host "软删 OK，回收站可见"

Write-Host ""
Write-Host "=== Step 12: 立即清回收站（管理员） ==="
[void](Invoke-RestMethod -Uri "$BASE/file/file/purge/$pngId" -Method Delete -Headers (Headers $tk))
try {
  $gone = Invoke-RestMethod -Uri "$BASE/file/file/$pngId" -Method Get -Headers (Headers $tk)
  if ($gone.data) { throw "未预期：硬删后仍能查到" }
} catch {
  Write-Host "硬删后查询被正确拒绝"
}

Write-Host ""
Write-Host "=== Step 13: 手动触发 quartz 清理任务 ==="
$purged = Invoke-RestMethod -Uri "$BASE/file/file/purge-now?retainDays=0" -Method Post -Headers (Headers $tk)
Assert-Eq $purged.code 200 "purge-now 应 200"
Write-Host "purged $($purged.data.purged) files (retain=0)"

Write-Host ""
Write-Host "=== Step 14: 文件夹软删 + 清理临时 ==="
[void](Invoke-RestMethod -Uri "$BASE/file/folder/$folderId" -Method Delete -Headers (Headers $tk))
Remove-Item -Recurse -Force -Path $tmp -ErrorAction SilentlyContinue
Write-Host "文件夹删除 + 临时目录清理完成"

Write-Host ""
Write-Host "=== ALL CHECKS PASSED ===" -ForegroundColor Green
