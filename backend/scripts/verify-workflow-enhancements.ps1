$ErrorActionPreference = "Stop"

# ====== Redis 工具（与 verify-data-scope 一致） ======
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

# ====== 1. admin 登录 ======
Write-Host "=== Step 1: admin login ==="
$adminTk = Login-As "admin" "admin123"
Write-Host "ok"

# ====== 2. 部署演示 BPMN（包含两个 userTask + 顺序流） ======
$bpmnXml = @'
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://scaffold/test"
             id="defs_demo_presign">
  <process id="demo_presign" name="presign+timeline demo" isExecutable="true">
    <startEvent id="StartEvent_1"/>
    <userTask id="Task_Apply" name="apply" flowable:assignee="1"/>
    <userTask id="Task_Approve" name="approve" flowable:assignee="1"/>
    <endEvent id="EndEvent_1"/>
    <sequenceFlow id="f1" sourceRef="StartEvent_1" targetRef="Task_Apply"/>
    <sequenceFlow id="f2" sourceRef="Task_Apply" targetRef="Task_Approve"/>
    <sequenceFlow id="f3" sourceRef="Task_Approve" targetRef="EndEvent_1"/>
  </process>
</definitions>
'@

$tempBpmn = Join-Path $env:TEMP "demo_presign.bpmn20.xml"
# 显式 UTF-8 (no BOM)：Flowable BPMN 解析对 BOM 不友好（"Content is not allowed in prolog"）
[System.IO.File]::WriteAllText($tempBpmn, $bpmnXml, [System.Text.UTF8Encoding]::new($false))

Write-Host ""
Write-Host "=== Step 2: deploy BPMN demo_presign ==="
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$bytes = [System.IO.File]::ReadAllBytes($tempBpmn)
$enc = [System.Text.Encoding]::GetEncoding('iso-8859-1')
$fileContent = $enc.GetString($bytes)
$bodyLines = (
  "--$boundary",
  'Content-Disposition: form-data; name="name"',
  '',
  'demo_presign',
  "--$boundary",
  'Content-Disposition: form-data; name="file"; filename="demo_presign.bpmn20.xml"',
  'Content-Type: application/xml',
  '',
  $fileContent,
  "--$boundary--",
  ''
) -join $LF

$h = Headers $adminTk
$h['Content-Type'] = "multipart/form-data; boundary=$boundary"
$deployResp = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/deployments" -Method Post -Body $bodyLines -Headers $h
Write-Host ("deploy raw response: " + ($deployResp | ConvertTo-Json -Depth 5))

# ====== 3. 启动一个流程实例 ======
Write-Host ""
Write-Host "=== Step 3: start instance ==="
$start = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ processDefinitionKey = "demo_presign"; businessKey = "E2E-001"; name = "前加签E2E" } | ConvertTo-Json)
$piId = $start.data.id
Write-Host ("instance id=$piId")

# 现在第一个任务（Task_Apply assignee=${initiator}=admin）已激活
$todo = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/todo" -Method Get -Headers (Headers $adminTk)
$applyTask = $todo.data | Where-Object { $_.taskDefinitionKey -eq "Task_Apply" } | Select-Object -First 1
if (-not $applyTask) { throw "Task_Apply not found in todo list (acquired ${($todo.data | ConvertTo-Json -Depth 5)})" }
Write-Host ("apply task id=$($applyTask.id) assignee=$($applyTask.assignee) processDefinitionKey=$($applyTask.processDefinitionKey)")

if ($applyTask.processDefinitionKey -ne "demo_presign") {
  throw "processDefinitionKey expected demo_presign, got '$($applyTask.processDefinitionKey)'"
}

# ====== 4. 给 Task_Approve 节点保存动态表单 schema ======
Write-Host ""
Write-Host "=== Step 4: save form schema for Task_Approve ==="
$schemaJson = @'
[
  {"type":"input","field":"reason","title":"审批意见","value":"","props":{"placeholder":"必填"},"validate":[{"required":true,"message":"必填"}]},
  {"type":"select","field":"result","title":"结论","value":"approved","options":[{"label":"同意","value":"approved"},{"label":"驳回","value":"rejected"}]}
]
'@
$schemaSave = Invoke-RestMethod -Uri "http://localhost:9080/workflow/form/schemas" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ processDefinitionKey="demo_presign"; activityId="Task_Approve"; name="审批表单"; schemaJson=$schemaJson } | ConvertTo-Json)
Write-Host ("form schema id=$($schemaSave.data.id) version=$($schemaSave.data.version)")

# 验证 active 拉得到
$active = Invoke-RestMethod -Uri "http://localhost:9080/workflow/form/schemas/active?processDefinitionKey=demo_presign&activityId=Task_Approve" `
  -Method Get -Headers (Headers $adminTk)
if (-not $active.data) { throw "active schema not found" }
Write-Host "active schema OK"

# ====== 5. 完成 Task_Apply（无 schema 走 plain）→ 推进到 Task_Approve ======
Write-Host ""
Write-Host "=== Step 5: complete Task_Apply ==="
[void](Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($applyTask.id)/complete" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ comment = "申请提交" } | ConvertTo-Json))

$todo2 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/todo" -Method Get -Headers (Headers $adminTk)
$approveTask = $todo2.data | Where-Object { $_.taskDefinitionKey -eq "Task_Approve" } | Select-Object -First 1
if (-not $approveTask) { throw "Task_Approve not found after applying; todo=$(($todo2.data | ConvertTo-Json -Depth 5))" }
Write-Host ("approve task id=$($approveTask.id)")

# ====== 6. 前加签：admin 在 Task_Approve 之前给 admin 自己加一个子任务 ======
# 因为我们的演示只有 admin 一个用户，给自己（"1"）加签会被后端拒绝（不能给自己前加签）；
# 这里给一个临时 userId "999" —— 不必真实存在，加签后我们用 timeline 拿到 childTaskId
Write-Host ""
Write-Host "=== Step 6: add-sign-before with helper assignee ==="
$presignBody = @{ assignee = "999"; comment = "presign by helper" } | ConvertTo-Json
[void](Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($approveTask.id)/add-sign-before" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body $presignBody)

# admin 的 todo 列表里仍然有 Task_Approve；尝试 complete 应被后端 ServiceException 阻塞。
# 后端在 ServiceException 时仍然返回 200 + AjaxResult{code=500/501}（GlobalExceptionHandler 通用响应）。
# 因此直接调用 RestMethod 看 .code/.msg 即可，不必用 catch。
Write-Host "expecting block when completing Task_Approve while child still alive..."
$blockedBody = @{ comment = "should fail"; formData = @{ reason="blocked"; result="approved" } } | ConvertTo-Json
$blockResp = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($approveTask.id)/complete" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body $blockedBody
Write-Host ("block response code=$($blockResp.code)")
# 中文在 PS 里会乱码，但只要 code != 200 且消息里带 "taskId=" 就说明被阻塞了
$blocked = ($blockResp.code -ne 200 -and $blockResp.msg -and $blockResp.msg.Contains("taskId="))
if (-not $blocked) { throw "Task_Approve was NOT blocked by pre-sign - expected non-200 with 'taskId=...'; got code=$($blockResp.code) msg-len=$(if ($blockResp.msg) { $blockResp.msg.Length } else { 0 })" }

# ====== 7. 把 presign_helper 的子任务转给 admin → 完成它 → 解阻 ======
# helper 用 admin 转办给 admin
Write-Host ""
Write-Host "=== Step 7: claim child task as admin ==="
# 现在 todo（assignee=admin）只看到 Task_Approve；而子任务 assignee=presign_helper
# 通过查询所有任务（跨 assignee）拿到子任务 id：用流程实例 timeline 里 task.addsign.before 事件取 childTaskId
$tl1 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piId/timeline" `
  -Method Get -Headers (Headers $adminTk)
$presignEvent = $tl1.data | Where-Object { $_.code -eq "task.addsign.before" } | Select-Object -First 1
if (-not $presignEvent) { throw "no task.addsign.before in timeline" }
$childId = $presignEvent.extra.childTaskId
Write-Host ("child task id=$childId")

# 子任务 assignee=999；后端 complete 内部会先 setAssignee(currentUser=1)，所以可以直接调 complete
$childCompleteResp = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$childId/complete" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ comment = "child done" } | ConvertTo-Json)
if ($childCompleteResp.code -ne 200) { throw "complete child failed: code=$($childCompleteResp.code)" }

Write-Host "child task done; original Task_Approve should now be unblocked"

# ====== 8. 用 formData 完成 Task_Approve ======
Write-Host ""
Write-Host "=== Step 8: complete Task_Approve with formData ==="
$completeBody = @{
  comment = "审批通过"
  formData = @{ reason = "AUTOTEST"; result = "approved" }
} | ConvertTo-Json
[void](Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($approveTask.id)/complete" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" -Body $completeBody)

# ====== 9. 拉 timeline，断言 含 task.complete + task.addsign.before + 至少一个 process.start / process.end ======
Write-Host ""
Write-Host "=== Step 9: verify timeline ==="
$tl = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piId/timeline" `
  -Method Get -Headers (Headers $adminTk)
$codes = $tl.data | ForEach-Object { $_.code } | Group-Object | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Host ("timeline codes: " + ($codes -join ", "))

$expected = @("process.start","activity.start","task.addsign.before","task.complete","process.end")
foreach ($e in $expected) {
  if (-not ($tl.data | Where-Object { $_.code -eq $e })) {
    throw "timeline missing expected entry: $e"
  }
}
Write-Host "timeline OK"

# ====== 10. 第二条实例：验证"撤销前加签" + "实例分页查询" ======
Write-Host ""
Write-Host "=== Step 10: cancel-presign + listInstances on a fresh instance ==="
$start2 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ processDefinitionKey = "demo_presign"; businessKey = "E2E-002"; name = "撤销前加签E2E" } | ConvertTo-Json)
$piId2 = $start2.data.id

# 推进到 Task_Approve
$todo3 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/todo" -Method Get -Headers (Headers $adminTk)
$apply2 = $todo3.data | Where-Object { $_.processInstanceId -eq $piId2 -and $_.taskDefinitionKey -eq "Task_Apply" } | Select-Object -First 1
[void](Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($apply2.id)/complete" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ comment = "申请2" } | ConvertTo-Json))

$todo4 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/todo" -Method Get -Headers (Headers $adminTk)
$approve2 = $todo4.data | Where-Object { $_.processInstanceId -eq $piId2 -and $_.taskDefinitionKey -eq "Task_Approve" } | Select-Object -First 1
if (-not $approve2) { throw "Task_Approve not found on E2E-002 todo list" }

# 在 Task_Approve 之前加签给 helper user 999
[void](Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($approve2.id)/add-sign-before" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ assignee = "999"; comment = "for cancel" } | ConvertTo-Json))

# 阻塞应生效：blockedByTaskIds 非空
$todo5 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/todo" -Method Get -Headers (Headers $adminTk)
$approveBlocked = $todo5.data | Where-Object { $_.id -eq $approve2.id } | Select-Object -First 1
if (-not $approveBlocked) { throw "Task_Approve disappeared after presign on E2E-002" }
if (-not ($approveBlocked.blockedByTaskIds -and $approveBlocked.blockedByTaskIds.Count -gt 0)) {
  throw "blockedByTaskIds not filled on TaskView after presign; got=$(($approveBlocked | ConvertTo-Json -Depth 5))"
}
Write-Host ("blockedByTaskIds=" + ($approveBlocked.blockedByTaskIds -join ","))

# 从 timeline 拿 childTaskId
$tl2 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piId2/timeline" `
  -Method Get -Headers (Headers $adminTk)
$presignEvent2 = $tl2.data | Where-Object { $_.code -eq "task.addsign.before" } | Select-Object -First 1
$childId2 = $presignEvent2.extra.childTaskId

# 撤销前加签（admin 即操作者，符合 operator 校验）
$cancelResp = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$childId2/add-sign-before" `
  -Method Delete -Headers (Headers $adminTk)
if ($cancelResp.code -ne 200) { throw "cancel-presign failed: code=$($cancelResp.code) msg=$($cancelResp.msg)" }
Write-Host "cancel-presign OK"

# 现在 Task_Approve 应被解除阻塞 → blockedByTaskIds 清空 → 可直接 complete
$todo6 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/todo" -Method Get -Headers (Headers $adminTk)
$approveAfterCancel = $todo6.data | Where-Object { $_.id -eq $approve2.id } | Select-Object -First 1
if (-not $approveAfterCancel) { throw "Task_Approve missing after cancel-presign" }
if ($approveAfterCancel.blockedByTaskIds -and $approveAfterCancel.blockedByTaskIds.Count -gt 0) {
  throw "Task_Approve is still blocked after cancel-presign: $(($approveAfterCancel.blockedByTaskIds -join ','))"
}
Write-Host "Task_Approve unblocked after cancel"

[void](Invoke-RestMethod -Uri "http://localhost:9080/workflow/task/$($approve2.id)/complete" `
  -Method Post -Headers (Headers $adminTk) -ContentType "application/json" `
  -Body (@{ comment = "已审批"; formData = @{ reason = "post-cancel"; result = "approved" } } | ConvertTo-Json))

# 时间轴上撤销标记应到位
$tl3 = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances/$piId2/timeline" `
  -Method Get -Headers (Headers $adminTk)
$cancelledEntry = $tl3.data | Where-Object { $_.code -eq "task.addsign.before" -and $_.extra.cancelled -eq $true } | Select-Object -First 1
if (-not $cancelledEntry) { throw "timeline did not record cancelled=true for the cancelled presign" }
Write-Host "timeline records cancelled=true"

# 实例总列表查询 —— admin 视角 + finished 状态可拿到 E2E-002
$page = Invoke-RestMethod -Uri "http://localhost:9080/workflow/process/instances?processDefinitionKey=demo_presign&businessKey=E2E-002&status=finished&pageNum=1&pageSize=10" `
  -Method Get -Headers (Headers $adminTk)
if (-not $page.rows -or $page.total -lt 1) { throw "instance search returned empty for E2E-002 finished; total=$($page.total)" }
$found = $page.rows | Where-Object { $_.id -eq $piId2 } | Select-Object -First 1
if (-not $found) { throw "E2E-002 instance not present in search result rows" }
Write-Host ("instance search OK; total=$($page.total) status=" + ($(if ($found.ended) { 'finished' } else { 'running' })))

# ====== 11. 收尾 ======
Write-Host ""
Write-Host "=== Done. All assertions passed. ==="
