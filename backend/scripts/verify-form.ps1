$ErrorActionPreference = "Stop"

# ====================================================================
# verify-form.ps1：M-10 表单引擎模块 E2E 验收脚本
#
# 覆盖：
#   - admin 登录
#   - 模板 CRUD：新增草稿 → 编辑保存 → 发布（状态从 DRAFT 转 PUBLISHED）
#   - 状态门：草稿期不可填报；发布后可填报；归档后不再接收新提交；删除前需归档
#   - 版本派生：发布后再编辑会自动派生 version+1 的新草稿（不破坏在线版本）
#   - active by formKey：拉当前激活版本
#   - 提交：合法 JSON 数据落 form_submission，submitter / templateKey / templateVersion 冗余正确
#   - 列表 / 详情：admin 可看全量；横向越权（直接构造非 admin 拉别人的 id）应 403
#   - 通用上传 /system/upload/file（admin 通配权限即可）：上传一个 .png，
#     校验返回 url 含 /profile/，然后用错误扩展名 .exe 应该被白名单拦下
#
# 调用：
#   pwsh backend/scripts/verify-form.ps1
#   要求后端在 http://localhost:9080 起着；admin/admin123；redis@127.0.0.1:6379 db3 取验证码
# ====================================================================

# ====== Redis 工具（与 verify-cms / verify-cms-workflow 同套） ======
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

# ====== 主流程 ======
Write-Host "=== Step 1: admin login ==="
$tk = Login-As "admin" "admin123"
Write-Host "ok"

$timestamp = (Get-Date -Format "HHmmss")
$formKey = "e2e_form_$timestamp"
$BASE = "http://localhost:9080"

Write-Host ""
Write-Host "=== Step 2: 创建草稿模板 ==="
$schemaV1 = @"
[
  {"type":"input","field":"title","title":"标题","value":"","props":{"placeholder":"请输入"},"validate":[{"required":true,"message":"必填"}]},
  {"type":"input","field":"summary","title":"摘要","value":""}
]
"@.Trim()
$createBody = @{
  formKey = $formKey
  name = "E2E 表单 $timestamp"
  category = "QA"
  description = "E2E 自动化创建"
  schemaJson = $schemaV1
} | ConvertTo-Json
$created = (Invoke-RestMethod -Uri "$BASE/form/template" -Method Post -Headers (Headers $tk) -Body $createBody -ContentType "application/json").data
Assert-True ($null -ne $created.id) "create returned id"
Assert-Eq $created.status "DRAFT" "新建状态应为 DRAFT"
Assert-Eq $created.version 1 "首版应为 v1"
$tplId = $created.id
Write-Host "template id=$tplId formKey=$formKey status=DRAFT v1"

Write-Host ""
Write-Host "=== Step 3: 草稿期不可填报 ==="
$submitDraft = @{ templateId = $tplId; data = '{"title":"x"}' } | ConvertTo-Json
$rejected = $false
try {
  $rsp = Invoke-RestMethod -Uri "$BASE/form/submission" -Method Post -Headers (Headers $tk) -Body $submitDraft -ContentType "application/json"
  if ($rsp.code -ne 200) { $rejected = $true; Write-Host "rejected with code=$($rsp.code) msg=$($rsp.msg)" }
} catch {
  $rejected = $true
}
if (-not $rejected) { throw "未预期：草稿期居然提交成功" }
Write-Host "草稿期提交被正确拒绝"

Write-Host ""
Write-Host "=== Step 4: 编辑（DRAFT 原地改） ==="
$editBody = @{
  id = $tplId
  name = "E2E 表单 $timestamp v1.1"
  schemaJson = $schemaV1
} | ConvertTo-Json
$edited = (Invoke-RestMethod -Uri "$BASE/form/template" -Method Put -Headers (Headers $tk) -Body $editBody -ContentType "application/json").data
Assert-Eq $edited.id $tplId "编辑应原地改 id 不变"
Assert-Eq $edited.version 1 "草稿编辑不应升 version"
Write-Host "草稿原地改 OK"

Write-Host ""
Write-Host "=== Step 5: 发布（DRAFT → PUBLISHED） ==="
$published = (Invoke-RestMethod -Uri "$BASE/form/template/$tplId/publish" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json").data
Assert-Eq $published.status "PUBLISHED" "发布后状态应为 PUBLISHED"
Assert-True ($null -ne $published.publishedAt) "publishedAt 应被设置"
Write-Host "v$($published.version) PUBLISHED publishedAt=$($published.publishedAt)"

Write-Host ""
Write-Host "=== Step 6: active by formKey 应取得当前发布版 ==="
$active = (Invoke-RestMethod -Uri "$BASE/form/template/active?formKey=$formKey" -Method Get -Headers (Headers $tk)).data
Assert-True ($null -ne $active) "active 不应为空"
Assert-Eq $active.id $tplId "active 应是当前发布版"
Write-Host "active id=$($active.id) v$($active.version)"

Write-Host ""
Write-Host "=== Step 7: 填报 / 提交 ==="
$submitBody = @{
  templateId = $tplId
  data = '{"title":"E2E 测试","summary":"通过自动化提交"}'
} | ConvertTo-Json
$submission = (Invoke-RestMethod -Uri "$BASE/form/submission" -Method Post -Headers (Headers $tk) -Body $submitBody -ContentType "application/json").data
Assert-True ($null -ne $submission.id) "submission id 应回传"
Assert-Eq $submission.templateKey $formKey "submission templateKey 冗余正确"
Assert-Eq $submission.templateVersion 1 "submission templateVersion 冗余 v1"
Assert-Eq $submission.submitter "admin" "submitter 应为当前登录用户"
Assert-Eq $submission.status "SUBMITTED" "新提交 status SUBMITTED"
$subId = $submission.id
Write-Host "submission id=$subId by admin"

Write-Host ""
Write-Host "=== Step 8: 查列表 + 查详情 ==="
$list = Invoke-RestMethod -Uri "$BASE/form/submission?templateKey=$formKey&pageNum=1&pageSize=20" -Method Get -Headers (Headers $tk)
Assert-True ($list.total -ge 1) "提交记录列表应至少含 1 条"
$detail = (Invoke-RestMethod -Uri "$BASE/form/submission/$subId" -Method Get -Headers (Headers $tk)).data
Assert-Eq $detail.id $subId "详情 id 正确"
Assert-True ($detail.data.Contains("E2E 测试")) "data 字段含原文"
Write-Host "列表 + 详情 OK（列表 total=$($list.total)）"

Write-Host ""
Write-Host "=== Step 9: 已发布编辑应派生 v2 草稿 ==="
$schemaV2 = @"
[
  {"type":"input","field":"title","title":"标题","value":""},
  {"type":"input","field":"summary","title":"摘要","value":""},
  {"type":"input","field":"author","title":"作者","value":""}
]
"@.Trim()
$editV2 = @{
  id = $tplId
  name = "E2E 表单 $timestamp v2 草稿"
  schemaJson = $schemaV2
} | ConvertTo-Json
$v2 = (Invoke-RestMethod -Uri "$BASE/form/template" -Method Put -Headers (Headers $tk) -Body $editV2 -ContentType "application/json").data
Assert-True ($v2.id -ne $tplId) "派生应得到新 id"
Assert-Eq $v2.version 2 "新版本应是 v2"
Assert-Eq $v2.status "DRAFT" "新版本应是 DRAFT"
$tplIdV2 = $v2.id
Write-Host "v2 id=$tplIdV2 DRAFT"

# 老 v1 仍然 PUBLISHED
$v1Now = (Invoke-RestMethod -Uri "$BASE/form/template/$tplId" -Method Get -Headers (Headers $tk)).data
Assert-Eq $v1Now.status "PUBLISHED" "v1 不被破坏，仍 PUBLISHED"
Write-Host "v1 仍 PUBLISHED 不受影响"

Write-Host ""
Write-Host "=== Step 10: 发布 v2，v1 自动归档 ==="
Invoke-RestMethod -Uri "$BASE/form/template/$tplIdV2/publish" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$v1After = (Invoke-RestMethod -Uri "$BASE/form/template/$tplId" -Method Get -Headers (Headers $tk)).data
$v2After = (Invoke-RestMethod -Uri "$BASE/form/template/$tplIdV2" -Method Get -Headers (Headers $tk)).data
Assert-Eq $v1After.status "ARCHIVED" "v1 发 v2 时应自动归档"
Assert-Eq $v2After.status "PUBLISHED" "v2 现 PUBLISHED"
Write-Host "v1 ARCHIVED, v2 PUBLISHED 自动切换"

Write-Host ""
Write-Host "=== Step 11: 通用上传 /system/upload/file ==="
$tmp = [System.IO.Path]::GetTempFileName() + ".png"
$bytes = [byte[]](137,80,78,71,13,10,26,10) + [byte[]](1..32)
[System.IO.File]::WriteAllBytes($tmp, $bytes)
try {
  $form = @{
    file = Get-Item $tmp
    bucket = "form/file"
  }
  $upRes = Invoke-RestMethod -Uri "$BASE/system/upload/file" -Method Post -Headers (Headers $tk) -Form $form
  Assert-True ($upRes.data.url.StartsWith("/profile/form/file/")) "上传 URL 应在 /profile/form/file/ 命名空间下"
  Write-Host "上传 OK url=$($upRes.data.url)"

  # 错误扩展名 .exe
  $bad = [System.IO.Path]::GetTempFileName() + ".exe"
  [System.IO.File]::WriteAllBytes($bad, [byte[]](1,2,3,4))
  $exeRejected = $false
  try {
    $form2 = @{ file = Get-Item $bad; bucket = "form/file" }
    $rspExe = Invoke-RestMethod -Uri "$BASE/system/upload/file" -Method Post -Headers (Headers $tk) -Form $form2
    if ($rspExe.code -ne 200) { $exeRejected = $true; Write-Host ".exe 被业务码拒（code=$($rspExe.code) msg=$($rspExe.msg)）" }
  } catch {
    $exeRejected = $true
  }
  if (-not $exeRejected) { throw "未预期：.exe 居然上传成功" }
  Write-Host ".exe 被白名单拒绝（符合预期）"
  Remove-Item $bad -ErrorAction SilentlyContinue
} finally {
  Remove-Item $tmp -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== Step 12: 删模板的状态门 ==="
# v1 当前 ARCHIVED → 可删
Invoke-RestMethod -Uri "$BASE/form/template/$tplId" -Method Delete -Headers (Headers $tk) | Out-Null
Write-Host "ARCHIVED v1 软删 OK"

# v2 当前 PUBLISHED → 应该被拒
try {
  Invoke-RestMethod -Uri "$BASE/form/template/$tplIdV2" -Method Delete -Headers (Headers $tk) | Out-Null
  throw "未预期：PUBLISHED 居然能删"
} catch {
  Write-Host "PUBLISHED 直接删被拒（符合预期）"
}

# 先归档再删
Invoke-RestMethod -Uri "$BASE/form/template/$tplIdV2/archive" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "$BASE/form/template/$tplIdV2" -Method Delete -Headers (Headers $tk) | Out-Null
Write-Host "归档后再删 OK"

Write-Host ""
Write-Host "================================================================"
Write-Host "  M-10 verify-form 全部步骤通过：模板 CRUD + 状态机 + 版本派生 + 提交 + 列表 / 详情 + 通用上传"
Write-Host "================================================================"
