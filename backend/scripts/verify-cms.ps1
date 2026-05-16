$ErrorActionPreference = "Stop"

# ====== Redis 工具（与 verify-data-scope.ps1 同套，简化登录） ======
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

# ====== 主流程 ======
Write-Host "=== Step 1: admin login ==="
$tk = Login-As "admin" "admin123"
Write-Host "ok"

Write-Host ""
Write-Host "=== Step 2: 创建栏目（外层 + 子栏目） ==="
$timestamp = (Get-Date -Format "HHmmss")
$rootCode = "e2e-news-$timestamp"
$childCode = "e2e-news-tech-$timestamp"

# 父栏目
$rootBody = @{ code = $rootCode; name = "E2E News $timestamp"; status = "0"; orderNum = 100 } | ConvertTo-Json
$rootRes = Invoke-RestMethod -Uri "http://localhost:9080/cms/channel" -Method Post -Headers (Headers $tk) -Body $rootBody -ContentType "application/json"
$rootId = $rootRes.data.id
Write-Host "root channel id=$rootId code=$rootCode"

# 子栏目
$childBody = @{ code = $childCode; name = "Tech"; parentId = $rootId; orderNum = 1 } | ConvertTo-Json
$childRes = Invoke-RestMethod -Uri "http://localhost:9080/cms/channel" -Method Post -Headers (Headers $tk) -Body $childBody -ContentType "application/json"
$childId = $childRes.data.id
Write-Host "child channel id=$childId code=$childCode parent=$rootId"

# 验证树查询
$tree = Invoke-RestMethod -Uri "http://localhost:9080/cms/channel/tree" -Method Get -Headers (Headers $tk)
$rootNode = $tree.data | Where-Object { $_.id -eq $rootId }
if (-not $rootNode) { throw "root channel not in tree" }
$childInTree = $rootNode.children | Where-Object { $_.id -eq $childId }
if (-not $childInTree) { throw "child channel missing under root in tree" }
Write-Host "tree contains parent + child as expected"

Write-Host ""
Write-Host "=== Step 3: 标签字典 ==="
$tagBody = @{ name = "e2e-tag-$timestamp"; color = "#3B82F6" } | ConvertTo-Json
$tag = (Invoke-RestMethod -Uri "http://localhost:9080/cms/tag" -Method Post -Headers (Headers $tk) -Body $tagBody -ContentType "application/json").data
$tagId = $tag.id
Write-Host "tag id=$tagId"

Write-Host ""
Write-Host "=== Step 4: 创建 4 篇文章覆盖状态机全分支 ==="

function Create-Article($title, $slugSuffix) {
  $body = @{
    channelId = $childId
    title = $title
    slug = "e2e-$slugSuffix-$timestamp"
    summary = "summary of $title"
    contentHtml = "<p>body of $title</p>"
    tagIds = @($tagId)
  } | ConvertTo-Json
  return (Invoke-RestMethod -Uri "http://localhost:9080/cms/article" -Method Post -Headers (Headers $tk) -Body $body -ContentType "application/json").data
}

$a1 = Create-Article "E2E Article 1 (final-publish)" "art1"
$a2 = Create-Article "E2E Article 2 (rejected)" "art2"
$a3 = Create-Article "E2E Article 3 (unpublished)" "art3"
$a4 = Create-Article "E2E Article 4 (back-to-draft)" "art4"
Write-Host "created article ids: $($a1.id), $($a2.id), $($a3.id), $($a4.id)"

# a1：DRAFT -> PENDING -> PUBLISHED
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a1.id)/submit" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a1.id)/approve" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$a1now = (Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a1.id)" -Method Get -Headers (Headers $tk)).data
Assert-Eq $a1now.status "PUBLISHED" "a1 should be PUBLISHED"
if (-not $a1now.publishedAt) { throw "a1 publishedAt should be set" }
Write-Host "a1 PUBLISHED publishedAt=$($a1now.publishedAt)"

# a2：DRAFT -> PENDING -> DRAFT (reject)
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a2.id)/submit" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$rejectBody = @{ reason = "格式不符" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a2.id)/reject" -Method Post -Headers (Headers $tk) -Body $rejectBody -ContentType "application/json" | Out-Null
$a2now = (Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a2.id)" -Method Get -Headers (Headers $tk)).data
Assert-Eq $a2now.status "DRAFT" "a2 should be DRAFT after reject"
Write-Host "a2 DRAFT after reject"

# a3：DRAFT -> PENDING -> PUBLISHED -> UNPUBLISHED
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)/submit" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)/approve" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)/unpublish" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$a3now = (Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)" -Method Get -Headers (Headers $tk)).data
Assert-Eq $a3now.status "UNPUBLISHED" "a3 should be UNPUBLISHED"
$a3publishedAt = $a3now.publishedAt
Write-Host "a3 UNPUBLISHED, publishedAt preserved=$a3publishedAt"

# a3 重新上线，publishedAt 不应被重置
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)/publish" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$a3now2 = (Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)" -Method Get -Headers (Headers $tk)).data
Assert-Eq $a3now2.status "PUBLISHED" "a3 should be PUBLISHED after republish"
Assert-Eq $a3now2.publishedAt $a3publishedAt "a3 publishedAt must NOT be reset on republish"
Write-Host "a3 republish keeps publishedAt"

# a4：DRAFT -> PENDING -> back-to-draft（直接退回）
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a4.id)/submit" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a4.id)/back-to-draft" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$a4now = (Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a4.id)" -Method Get -Headers (Headers $tk)).data
Assert-Eq $a4now.status "DRAFT" "a4 should be DRAFT after back-to-draft"
Write-Host "a4 back-to-draft works"

Write-Host ""
Write-Host "=== Step 5: 公开 API（匿名）只能看到 PUBLISHED ==="
$pub = Invoke-RestMethod -Uri "http://localhost:9080/cms/public/articles?channelId=$childId&pageSize=50" -Method Get
$pubIds = $pub.rows | ForEach-Object { $_.id }
Write-Host "public ids under channel: $($pubIds -join ',')"
if ($pubIds -contains $a2.id) { throw "public list leaked DRAFT article a2" }
if ($pubIds -contains $a4.id) { throw "public list leaked DRAFT article a4" }
if ($pubIds -notcontains $a1.id) { throw "public list missing PUBLISHED article a1" }
if ($pubIds -notcontains $a3.id) { throw "public list missing republished article a3" }
Write-Host "public list filter PUBLISHED only OK"

# slug 详情 + 阅读量自增
$beforeView = $a1now.viewCount
$detailRes = Invoke-RestMethod -Uri "http://localhost:9080/cms/public/articles/$($a1.slug)" -Method Get
Assert-Eq $detailRes.data.id $a1.id "public bySlug returns correct article"
$a1after = (Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a1.id)" -Method Get -Headers (Headers $tk)).data
if ($a1after.viewCount -le $beforeView) { throw "public bySlug should increment viewCount (before=$beforeView after=$($a1after.viewCount))" }
Write-Host "public bySlug viewCount incremented: $beforeView -> $($a1after.viewCount)"

# 公开 API 不应返回 UNPUBLISHED：把 a3 重新下线再校验
Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$($a3.id)/unpublish" -Method Post -Headers (Headers $tk) -Body "{}" -ContentType "application/json" | Out-Null
$pub2 = Invoke-RestMethod -Uri "http://localhost:9080/cms/public/articles?channelId=$childId&pageSize=50" -Method Get
$pubIds2 = $pub2.rows | ForEach-Object { $_.id }
if ($pubIds2 -contains $a3.id) { throw "public list leaked UNPUBLISHED article a3" }
Write-Host "public list excludes UNPUBLISHED OK"

Write-Host ""
Write-Host "=== Step 6: 关键词搜索 ==="
$kwRes = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/list?keyword=Article%202&pageSize=20" -Method Get -Headers (Headers $tk)
$kwIds = $kwRes.rows | ForEach-Object { $_.id }
if ($kwIds -notcontains $a2.id) { throw "keyword search did not find article 2" }
Write-Host "keyword search hit a2 ($($a2.title))"

Write-Host ""
Write-Host "=== Step 7: 不能删除还有文章的栏目 ==="
$delErr = $false
try {
  $rsp = Invoke-RestMethod -Uri "http://localhost:9080/cms/channel/$childId" -Method Delete -Headers (Headers $tk)
  if ($rsp.code -ne 200) { $delErr = $true; Write-Host "rejected with code=$($rsp.code) msg=$($rsp.msg)" }
} catch {
  $delErr = $true
}
if (-not $delErr) { throw "delete channel with articles should fail" }
Write-Host "channel with articles cannot be deleted (OK)"

Write-Host ""
Write-Host "=== Step 8: 软删全部文章 + 删除栏目 ==="
foreach ($id in @($a1.id, $a2.id, $a3.id, $a4.id)) {
  Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$id" -Method Delete -Headers (Headers $tk) | Out-Null
}
Invoke-RestMethod -Uri "http://localhost:9080/cms/channel/$childId" -Method Delete -Headers (Headers $tk) | Out-Null
Invoke-RestMethod -Uri "http://localhost:9080/cms/channel/$rootId" -Method Delete -Headers (Headers $tk) | Out-Null
Write-Host "channels cleaned"

# 软删的文章不能再被公开 API 看到
$pub3 = Invoke-RestMethod -Uri "http://localhost:9080/cms/public/articles?channelId=$childId&pageSize=50" -Method Get
if ($pub3.rows.Count -gt 0) {
  $pubIds3 = $pub3.rows | ForEach-Object { $_.id }
  if (($pubIds3 | Where-Object { @($a1.id, $a3.id) -contains $_ }).Count -gt 0) {
    throw "soft-deleted articles leaked to public list"
  }
}
Write-Host "soft-delete excluded from public list OK"

# 标签清理
Invoke-RestMethod -Uri "http://localhost:9080/cms/tag/$tagId" -Method Delete -Headers (Headers $tk) | Out-Null
Write-Host "tag cleaned"

Write-Host ""
Write-Host "=== ALL CMS E2E CHECKS PASSED ==="
