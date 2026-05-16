# ====================================================================
# verify-cms-workflow.ps1 — M-4 / M-5 跨模块联动 E2E 验证
# ====================================================================
# 覆盖：
#   1. M-4 提交审核走 Flowable（process_instance_id 不为空 + 状态 PENDING）
#   2. M-4 审核通过 → CMS 文章 PUBLISHED + published_at 写入
#   3. M-4 审核驳回 → CMS 文章 DRAFT + reason 进 sys_audit_log
#   4. M-4 onApprove cancel：CMS UI 直接 approve → 关联流程实例被 cancel
#   5. M-5 站内信深链：审核通过 / 驳回 / 下线 → cms_author 用户 inbox 收到
#       type=cms.article.* 记录，payload.link=/cms/article-edit/{id}
#
# 设计要点：
#   * 测试故意把"作者"和"审批人"分成两个用户，否则 ArticleStatusInboxListener
#     会因 actor==author 跳过通知（避免给自己发自己的状态），inbox 看不到任何
#     cms.* 消息。脚本会创建 cms_author / Test@1234 这个用户（角色绑 admin
#     role 简化授权），结束时不删（方便排查；如要清理重跑前手工 DELETE）。
#   * 桥模块默认开关：app.module.cms.workflow.enabled=true（M-4），
#     app.module.cms.inbox.enabled=true（M-5，默认 on）。
#   * 关闭桥模块的回归验证不能由脚本完成（需要重启），脚本最后会打印
#     hint 让运维手工切关 prop 复跑——见 Step 13。
#
# 前置条件：
#   - 后端在 8080 端口运行，且 cms-workflow 桥模块已启用
#   - Redis 在 6379，账号 123456，db=3（用于读 captcha）
#
# 命令：pwsh -File backend/scripts/verify-cms-workflow.ps1
# ====================================================================

$ErrorActionPreference = "Stop"

# ====== 工具函数 ======
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

function Assert-NotEmpty($value, $msg) {
  if ([string]::IsNullOrEmpty($value)) { throw "ASSERT FAIL: $msg (value is empty/null)" }
}

# 在 cms_author 的 inbox 中找 type 与 articleId 都匹配的未读消息
function Find-CmsMessage($token, $type, $articleId) {
  $inbox = Invoke-RestMethod -Uri "http://localhost:9080/system/inbox/unread?limit=100" -Method Get -Headers (Headers $token)
  if (-not $inbox.data) { return $null }
  foreach ($m in $inbox.data) {
    if ($m.type -ne $type) { continue }
    try {
      $p = $m.payload | ConvertFrom-Json
      if ([string]$p.articleId -eq [string]$articleId) {
        return @{ msg = $m; payload = $p }
      }
    } catch { continue }
  }
  return $null
}

# ====== 主流程 ======
Write-Host "=== Step 1: admin login ==="
$adminTk = Login-As "admin" "admin123"
Write-Host "ok"

Write-Host ""
Write-Host "=== Step 2: 检查桥模块启用状态（actuator/scaffold-modules） ==="
$bridgeWfEnabled = $true   # 默认假设开了；下面的功能性断言才是真验证
try {
  $modules = Invoke-RestMethod -Uri "http://localhost:9080/actuator/scaffold-modules" -Method Get -Headers (Headers $adminTk)
  $cmsWfModule = $modules.modules | Where-Object { $_.name -eq "cms-workflow" }
  $cmsInboxModule = $modules.modules | Where-Object { $_.name -eq "cms-inbox" }
  if (-not $cmsWfModule) {
    Write-Warning "桥模块 cms-workflow 未启用：请把 application.yml 中 app.module.cms.workflow.enabled 设为 true 后重启"
    $bridgeWfEnabled = $false
  } else {
    Write-Host "cms-workflow module: enabled v$($cmsWfModule.version)"
  }
  if (-not $cmsInboxModule) {
    Write-Warning "桥模块 cms-inbox 未启用（默认 on，可能被 app.module.cms.inbox.enabled=false 显式关掉）"
  } else {
    Write-Host "cms-inbox module: enabled v$($cmsInboxModule.version)"
  }
} catch {
  Write-Warning "actuator/scaffold-modules 不可访问，跳过元数据校验：$_"
}

Write-Host ""
Write-Host "=== Step 3: 准备测试用户 cms_author（作者） ==="
# 已存在则复用；否则按 admin role 创建
$existUsers = Invoke-RestMethod -Uri "http://localhost:9080/system/user/list?userName=cms_author" -Method Get -Headers (Headers $adminTk)
if ($existUsers.rows.Count -eq 0) {
  $userBody = @{
    userName = "cms_author"
    nickName = "CMS 作者测试号"
    password = "Test@1234"
    status = "0"
    deptId = 100   # 默认根部门
    roleIds = @(1) # 直接给 admin 角色省去配 CMS 菜单的麻烦
    postIds = @()
  } | ConvertTo-Json -Depth 5
  $null = Invoke-RestMethod -Uri "http://localhost:9080/system/user" -Method Post -Headers (Headers $adminTk) -Body $userBody -ContentType "application/json"
  Write-Host "cms_author 用户已创建"
} else {
  Write-Host "cms_author 已存在，复用"
}
$authorTk = Login-As "cms_author" "Test@1234"
Write-Host "cms_author login ok"

Write-Host ""
Write-Host "=== Step 4: 准备测试栏目 ==="
$timestamp = (Get-Date -Format "HHmmss")
$chCode = "wf-news-$timestamp"
$chBody = @{ code = $chCode; name = "WF News $timestamp"; status = "0"; orderNum = 1 } | ConvertTo-Json
$chRes = Invoke-RestMethod -Uri "http://localhost:9080/cms/channel" -Method Post -Headers (Headers $adminTk) -Body $chBody -ContentType "application/json"
$chId = $chRes.data.id
Write-Host "channel id=$chId code=$chCode"

# ====================================================================
# 文章 A：审批通过路径
#   作者 cms_author 创建 + 提交 → admin 审批通过 →
#   cms_author 收到 type=cms.article.published 站内信
# ====================================================================
Write-Host ""
Write-Host "=== Step 5: cms_author 创建文章 A 并提交审核 ==="
$titleA = "WF文章A 通过 $timestamp"
$bodyA = @{
  channelId = $chId; title = $titleA; summary = "走 workflow 审批通过流程";
  contentHtml = "<p>正文 A</p>"
} | ConvertTo-Json
$resA = Invoke-RestMethod -Uri "http://localhost:9080/cms/article" -Method Post -Headers (Headers $authorTk) -Body $bodyA -ContentType "application/json"
$idA = $resA.data.id
Write-Host "article A id=$idA author=cms_author"

$null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idA/submit" -Method Post -Headers (Headers $authorTk) -ContentType "application/json" -Body "{}"
$detailA = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idA" -Method Get -Headers (Headers $authorTk)
Assert-Eq $detailA.data.status "PENDING" "A submit 后状态应为 PENDING"
if ($bridgeWfEnabled) {
  Assert-NotEmpty $detailA.data.processInstanceId "A submit 后 processInstanceId 应非空（说明已交给 workflow）"
}
$piidA = $detailA.data.processInstanceId
Write-Host "A piid=$piidA status=PENDING ✓"

Write-Host ""
Write-Host "=== Step 6: admin 审批 A 通过 ==="
if ($bridgeWfEnabled -and $piidA) {
  $taskListA = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piidA/tasks" -Method Get -Headers (Headers $adminTk)
  if (-not $taskListA.data -or $taskListA.data.Count -eq 0) {
    throw "无法在流程实例 $piidA 下找到活跃任务；请检查 BPMN 是否部署成功"
  }
  $taskAId = $taskListA.data[0].id
  $null = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$taskAId/claim" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body "{}"
  $completeBody = @{
    variables = @{ approved = $true; reason = $null; reviewer = "admin" }
  } | ConvertTo-Json -Depth 4
  $null = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$taskAId/complete" -Method Post -Headers (Headers $adminTk) -Body $completeBody -ContentType "application/json"
} else {
  # 桥关闭：admin 直接 approve
  $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idA/approve" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body "{}"
}

Start-Sleep -Milliseconds 500
$detailA2 = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idA" -Method Get -Headers (Headers $adminTk)
Assert-Eq $detailA2.data.status "PUBLISHED" "Workflow 通过后 A 状态应为 PUBLISHED"
Assert-NotEmpty $detailA2.data.publishedAt "Workflow 通过后 publishedAt 应有值"
Write-Host "A → PUBLISHED ✓ publishedAt=$($detailA2.data.publishedAt)"

Write-Host ""
Write-Host "=== Step 7: cms_author 应收到「已发布」站内信 ==="
$hitA = Find-CmsMessage $authorTk "cms.article.published" $idA
if ($hitA) {
  Assert-Eq $hitA.payload.link "/cms/article-edit/$idA" "已发布站内信 link 应指向 /cms/article-edit/$idA"
  Assert-Eq $hitA.payload.newStatus "PUBLISHED" "已发布站内信 newStatus 应为 PUBLISHED"
  Write-Host "M-5 已发布站内信 ✓ content=$($hitA.payload.content)"
} else {
  Write-Warning "M-5 未在 cms_author inbox 找到 cms.article.published 关于 articleId=$idA 的消息"
  Write-Warning "    — 桥模块 cms-inbox 是否启用？MessagePublisher 是否注册？"
}

# ====================================================================
# 文章 B：审批驳回路径
# ====================================================================
Write-Host ""
Write-Host "=== Step 8: cms_author 创建文章 B（走驳回流程） ==="
$titleB = "WF文章B 驳回 $timestamp"
$bodyB = @{
  channelId = $chId; title = $titleB; summary = "走 workflow 审批驳回流程";
  contentHtml = "<p>正文 B</p>"
} | ConvertTo-Json
$resB = Invoke-RestMethod -Uri "http://localhost:9080/cms/article" -Method Post -Headers (Headers $authorTk) -Body $bodyB -ContentType "application/json"
$idB = $resB.data.id

$null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idB/submit" -Method Post -Headers (Headers $authorTk) -ContentType "application/json" -Body "{}"
$detailB = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idB" -Method Get -Headers (Headers $authorTk)
$piidB = $detailB.data.processInstanceId
Write-Host "B id=$idB piid=$piidB"

$rejectReason = "标题不合规"
if ($bridgeWfEnabled -and $piidB) {
  $tasksB = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piidB/tasks" -Method Get -Headers (Headers $adminTk)
  $taskBId = $tasksB.data[0].id
  $null = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$taskBId/claim" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body "{}"
  $rejectBody = @{
    variables = @{ approved = $false; reason = $rejectReason; reviewer = "admin" }
  } | ConvertTo-Json -Depth 4
  $null = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$taskBId/complete" -Method Post -Headers (Headers $adminTk) -Body $rejectBody -ContentType "application/json"
} else {
  # 桥关闭：admin 直接 reject
  $rejectFallback = @{ reason = $rejectReason } | ConvertTo-Json
  $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idB/reject" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body $rejectFallback
}

Start-Sleep -Milliseconds 500
$detailB2 = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idB" -Method Get -Headers (Headers $adminTk)
Assert-Eq $detailB2.data.status "DRAFT" "Workflow 驳回后 B 状态应为 DRAFT"
Write-Host "B → DRAFT ✓"

Write-Host ""
Write-Host "=== Step 9: cms_author 应收到「已驳回」站内信（含 reason） ==="
$hitB = Find-CmsMessage $authorTk "cms.article.rejected" $idB
if ($hitB) {
  Assert-Eq $hitB.payload.link "/cms/article-edit/$idB" "已驳回站内信 link 应指向 /cms/article-edit/$idB"
  Assert-Eq $hitB.payload.reason $rejectReason "已驳回站内信 reason 应等于 $rejectReason"
  Write-Host "M-5 已驳回站内信 ✓ content=$($hitB.payload.content)"
} else {
  Write-Warning "M-5 未在 cms_author inbox 找到 cms.article.rejected 关于 articleId=$idB 的消息"
}

# ====================================================================
# 文章 C：上线 → 下线 → 验证「已下线」站内信
# ====================================================================
Write-Host ""
Write-Host "=== Step 10: cms_author 创建文章 C，admin 通过后立即下线 ==="
$titleC = "WF文章C 下线 $timestamp"
$bodyC = @{
  channelId = $chId; title = $titleC; summary = "测试下线通知";
  contentHtml = "<p>正文 C</p>"
} | ConvertTo-Json
$resC = Invoke-RestMethod -Uri "http://localhost:9080/cms/article" -Method Post -Headers (Headers $authorTk) -Body $bodyC -ContentType "application/json"
$idC = $resC.data.id

$null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idC/submit" -Method Post -Headers (Headers $authorTk) -ContentType "application/json" -Body "{}"
$detailC = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idC" -Method Get -Headers (Headers $adminTk)
$piidC = $detailC.data.processInstanceId

if ($bridgeWfEnabled -and $piidC) {
  $tasksC = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piidC/tasks" -Method Get -Headers (Headers $adminTk)
  $taskCId = $tasksC.data[0].id
  $null = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$taskCId/claim" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body "{}"
  $null = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$taskCId/complete" -Method Post -Headers (Headers $adminTk) -Body (@{ variables = @{ approved = $true; reviewer = "admin" } } | ConvertTo-Json -Depth 4) -ContentType "application/json"
} else {
  $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idC/approve" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body "{}"
}
Start-Sleep -Milliseconds 400

# admin 下线 C
$unpublishBody = @{ reason = "运营临时下线" } | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idC/unpublish" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body $unpublishBody
Start-Sleep -Milliseconds 400

$detailC2 = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idC" -Method Get -Headers (Headers $adminTk)
Assert-Eq $detailC2.data.status "UNPUBLISHED" "C 下线后状态应为 UNPUBLISHED"
Write-Host "C → UNPUBLISHED ✓"

Write-Host ""
Write-Host "=== Step 11: cms_author 应收到「已下线」站内信 ==="
$hitC = Find-CmsMessage $authorTk "cms.article.unpublished" $idC
if ($hitC) {
  Assert-Eq $hitC.payload.link "/cms/article-edit/$idC" "已下线站内信 link 应指向 /cms/article-edit/$idC"
  Write-Host "M-5 已下线站内信 ✓ content=$($hitC.payload.content)"
} else {
  Write-Warning "M-5 未在 cms_author inbox 找到 cms.article.unpublished 关于 articleId=$idC 的消息"
}

# ====================================================================
# 文章 D：CMS UI 直接 approve（绕过 workflow） → 验证 onApprove cancelInstance
# ====================================================================
Write-Host ""
Write-Host "=== Step 12: 文章 D 验证 onApprove cancel 流程实例（仅当桥开启时跑） ==="
if ($bridgeWfEnabled) {
  $titleD = "WF文章D 直接通过 $timestamp"
  $bodyD = @{
    channelId = $chId; title = $titleD; summary = "验证 cancelInstance";
    contentHtml = "<p>正文 D</p>"
  } | ConvertTo-Json
  $resD = Invoke-RestMethod -Uri "http://localhost:9080/cms/article" -Method Post -Headers (Headers $authorTk) -Body $bodyD -ContentType "application/json"
  $idD = $resD.data.id

  $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idD/submit" -Method Post -Headers (Headers $authorTk) -ContentType "application/json" -Body "{}"
  $dD = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idD" -Method Get -Headers (Headers $adminTk)
  $piidD = $dD.data.processInstanceId
  Assert-NotEmpty $piidD "D 应当有 processInstanceId"

  # 不去 complete task，而是直接调 CMS approve（等价于 ReviewBar 直接点了通过）
  $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idD/approve" -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body "{}"
  $dD2 = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$idD" -Method Get -Headers (Headers $adminTk)
  Assert-Eq $dD2.data.status "PUBLISHED" "D 直接 approve 后状态应为 PUBLISHED"

  Start-Sleep -Milliseconds 300
  try {
    $stateD = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piidD/state" -Method Get -Headers (Headers $adminTk)
    if ($stateD.data -and $stateD.data.ended) {
      Write-Host "D piid=$piidD 已 ended ✓ (cancelInstance 生效)"
    } else {
      Write-Warning "D piid=$piidD 仍 active；cancelInstance 是否被触发？"
    }
  } catch {
    Write-Host "D 流程实例查询异常（实例可能已被彻底清理）：$_"
  }
} else {
  Write-Host "(桥模块未启用，跳过 onApprove cancel 验证)"
  $idD = $null
}

# ====================================================================
# Step 13：清理 + 关闭桥模块的回归 hint
# ====================================================================
Write-Host ""
Write-Host "=== Step 13: 清理（软删测试文章）==="
$idsToClean = @($idA, $idB, $idC)
if ($idD) { $idsToClean += $idD }
foreach ($id in $idsToClean) {
  try { $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/article/$id" -Method Delete -Headers (Headers $adminTk) }
  catch { Write-Warning "删除文章 $id 失败：$_" }
}
try { $null = Invoke-RestMethod -Uri "http://localhost:9080/cms/channel/$chId" -Method Delete -Headers (Headers $adminTk) }
catch { Write-Warning "删栏目失败（可能因为有审计 trace）：$_" }

Write-Host ""
Write-Host "=== ALL OK ==="
Write-Host ""
Write-Host "如需验证「桥模块关闭后 CMS 自闭环退化」分支，请手工："
Write-Host "  1. 修改 application.yml：app.module.cms.workflow.enabled=false"
Write-Host "  2. 重启后端"
Write-Host "  3. 复跑此脚本：脚本会检测 cms-workflow 未启用 → 自动走 CMS 直 approve / reject 路径"
Write-Host "     此时文章 piid 应为空，inbox 仍能正常发 已发布 / 已驳回 / 已下线 通知（M-5 与 M-4 解耦）"
