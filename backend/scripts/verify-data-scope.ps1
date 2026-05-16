$ErrorActionPreference = "Stop"

# ====== Redis 工具 ======
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

# ====== 主流程 ======
Write-Host "=== Step 1: admin login ==="
$adminTk = Login-As "admin" "admin123"
Write-Host "ok"

Write-Host ""
Write-Host "=== Step 2: pick a non-root department ==="
$deptList = Invoke-RestMethod -Uri "http://localhost:9080/system/dept/list" -Method Get -Headers (Headers $adminTk)
# 取一个非根部门（parentId != 0）
$targetDept = $deptList.data | Where-Object { $_.parentId -ne 0 } | Select-Object -First 1
if (-not $targetDept) { throw "no non-root dept" }
Write-Host ("target dept: id={0} name={1} parentId={2}" -f $targetDept.deptId, $targetDept.deptName, $targetDept.parentId)
$targetDeptId = $targetDept.deptId

Write-Host ""
Write-Host "=== Step 3: create role with dataScope=3 (deptOnly) and permission=system:audit:list ==="
# 用 admin 给该角色挂 system:audit:list 菜单 → 角色才会被切面参与拼 SQL（permission 校验）
# 简化：roleKey 与 permission 匹配规则，此处 permissions 在角色对象由后端计算；前端创建角色不直接写 permissions。
# 但 DataScopeAspect 里 permission 串和 role.getPermissions() 比较——getPermissions 是动态从菜单算的。
# 为了让切面 hit 上"本部门"分支，最稳是不传 permission 字符串。但 controller 上的 PermissionContextHolder 会带上 'system:audit:list'，
# 角色 permissions 必须包含这个串。
# 流程：1) 创建角色 dataScope=3，绑定一个含 system:audit:list 的菜单（直接绑 1037 即"操作日志"——不行，那是 monitor:operlog）
# 看下 sys_menu 中 system:audit:* 菜单在哪
$menus = Invoke-RestMethod -Uri "http://localhost:9080/system/menu/list?perms=system:audit:list" -Method Get -Headers (Headers $adminTk)
$auditMenu = $menus.data | Where-Object { $_.perms -eq "system:audit:list" } | Select-Object -First 1
if (-not $auditMenu) { throw "menu 'system:audit:list' not found" }
Write-Host ("audit menu id={0} name={1}" -f $auditMenu.menuId, $auditMenu.menuName)

$roleBody = @{
  roleName = "ds-test-deptonly"
  roleKey  = "ds_test_deptonly"
  roleSort = 99
  status   = "0"
  dataScope = "3"
  menuCheckStrictly = $false
  deptCheckStrictly = $false
  menuIds = @($auditMenu.menuId)
  remark = "data-scope demo"
} | ConvertTo-Json -Depth 5

# 如果已经存在，先删掉
$existRoles = Invoke-RestMethod -Uri "http://localhost:9080/system/role/list?roleKey=ds_test_deptonly&pageSize=10" -Method Get -Headers (Headers $adminTk)
foreach ($r in $existRoles.rows) {
  Write-Host "deleting existing role $($r.roleId) $($r.roleKey)"
  Invoke-RestMethod -Uri "http://localhost:9080/system/role/$($r.roleId)" -Method Delete -Headers (Headers $adminTk) | Out-Null
}

$createRoleResp = Invoke-RestMethod -Uri "http://localhost:9080/system/role" -Method Post -Headers (Headers $adminTk) -Body $roleBody -ContentType "application/json"
Write-Host "create role:" ($createRoleResp | ConvertTo-Json -Depth 3)
$rolesAfter = Invoke-RestMethod -Uri "http://localhost:9080/system/role/list?roleKey=ds_test_deptonly&pageSize=10" -Method Get -Headers (Headers $adminTk)
$newRole = $rolesAfter.rows[0]
$newRoleId = $newRole.roleId
Write-Host "new roleId: $newRoleId"

Write-Host ""
Write-Host "=== Step 4: create test user 'ds_user' under target dept, assigned to new role ==="
$existUsers = Invoke-RestMethod -Uri "http://localhost:9080/system/user/list?userName=ds_user" -Method Get -Headers (Headers $adminTk)
foreach ($u in $existUsers.rows) {
  Write-Host "deleting existing user $($u.userId) $($u.userName)"
  Invoke-RestMethod -Uri "http://localhost:9080/system/user/$($u.userId)" -Method Delete -Headers (Headers $adminTk) | Out-Null
}

$userBody = @{
  userName = "ds_user"
  nickName = "数据范围测试账号"
  password = "Test@1234"
  status = "0"
  deptId = $targetDeptId
  roleIds = @($newRoleId)
  postIds = @()
} | ConvertTo-Json -Depth 5

$createUserResp = Invoke-RestMethod -Uri "http://localhost:9080/system/user" -Method Post -Headers (Headers $adminTk) -Body $userBody -ContentType "application/json"
Write-Host "create user:" ($createUserResp | ConvertTo-Json -Depth 3)

Write-Host ""
Write-Host "=== Step 5: ds_user login, expects only audit logs whose actor_dept_id = $targetDeptId ==="
$dsTk = Login-As "ds_user" "Test@1234"
$dsAudit = Invoke-RestMethod -Uri "http://localhost:9080/system/audit/log/list?pageSize=50" -Method Get -Headers (Headers $dsTk)
Write-Host "ds_user sees rows = $($dsAudit.rows.Count) / total = $($dsAudit.total)"

Write-Host ""
Write-Host "=== Step 6: admin still sees full list ==="
$adminAudit2 = Invoke-RestMethod -Uri "http://localhost:9080/system/audit/log/list?pageSize=50" -Method Get -Headers (Headers $adminTk)
Write-Host "admin sees rows = $($adminAudit2.rows.Count) / total = $($adminAudit2.total)"

Write-Host ""
Write-Host "=== Step 7: have ds_user trigger an action that writes to audit log (by changing own data) ==="
# ds_user 只有 system:audit:list 权限，没有写入审计的能力。
# 改用：admin 替 ds_user 模拟一次"自己部门发生的审计事件" → 给 ds_user 改昵称（sys.user UPDATE 会写 sys_audit_log，actor_dept_id 是 admin 部门）。
# 这样不行，actor 是 admin。要让 actor_dept_id 命中 ds_user 部门，必须 ds_user 自己触发能写审计的接口。
# 唯一不需要写权限的"会被审计"的接口比较少。退而求其次：让 admin 创建另一条审计记录 actor=ds_user，actor_dept_id=$targetDeptId，
# 然后用 ds_user 验证能看到。这一步只能直接验证 SQL 过滤：admin 看到 N 行，ds_user 看到 M 行（M 是 actor_dept_id=$targetDeptId 的子集）。
Write-Host "(ds_user has read-only @ system:audit:list, hard to write audit through HTTP; rely on filter assertion)"

Write-Host ""
Write-Host "=== Result Summary ==="
Write-Host "admin total = $($adminAudit2.total)"
Write-Host "ds_user (deptOnly, dept=$targetDeptId) total = $($dsAudit.total)"
$expected = 0
foreach ($r in $adminAudit2.rows) {
  # rows 字段里没暴露 actor_dept_id；从 detail 拉
  $det = Invoke-RestMethod -Uri "http://localhost:9080/system/audit/log/$($r.id)" -Method Get -Headers (Headers $adminTk)
  if ($det.data.actorDeptId -eq $targetDeptId) { $expected++ }
}
Write-Host "expected (admin rows whose actor_dept_id=$targetDeptId) = $expected"
if ($dsAudit.total -eq $expected) {
  Write-Host "[PASS] ds_user (dataScope=3) sees exactly the rows with actor_dept_id=$targetDeptId"
} else {
  Write-Host "[FAIL] ds_user.total=$($dsAudit.total) but expected=$expected"
}

Write-Host ""
Write-Host "=== Step 8: switch role to dataScope=1 (ALL), ds_user should see full list ==="
$dsScopeBody = @{
  roleId = $newRoleId
  dataScope = "1"
  deptIds = @()
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:9080/system/role/dataScope" -Method Put -Headers (Headers $adminTk) -Body $dsScopeBody -ContentType "application/json" | Out-Null
# ds_user 已登录态在 token 里，要重新登录拿到最新角色
$dsTk2 = Login-As "ds_user" "Test@1234"
$dsAuditAll = Invoke-RestMethod -Uri "http://localhost:9080/system/audit/log/list?pageSize=50" -Method Get -Headers (Headers $dsTk2)
Write-Host "ds_user (dataScope=1) total = $($dsAuditAll.total) ; admin total = $($adminAudit2.total)"
if ($dsAuditAll.total -ge $adminAudit2.total) {
  Write-Host "[PASS] dataScope=1 grants full visibility (>= admin baseline)"
} else {
  Write-Host "[FAIL] dataScope=1 should see >= admin baseline"
}

Write-Host ""
Write-Host "=== Step 9: switch role to dataScope=5 SELF only - ds_user should see only own actor records ==="
$dsScopeBody2 = @{
  roleId = $newRoleId
  dataScope = "5"
  deptIds = @()
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:9080/system/role/dataScope" -Method Put -Headers (Headers $adminTk) -Body $dsScopeBody2 -ContentType "application/json" | Out-Null
$dsTk3 = Login-As "ds_user" "Test@1234"
$dsAuditSelf = Invoke-RestMethod -Uri "http://localhost:9080/system/audit/log/list?pageSize=50" -Method Get -Headers (Headers $dsTk3)
Write-Host "ds_user (dataScope=5 self) total = $($dsAuditSelf.total)"
$nonSelfRows = 0
foreach ($r in $dsAuditSelf.rows) { if ($r.actor -ne "ds_user") { $nonSelfRows++ } }
if ($nonSelfRows -eq 0) {
  Write-Host "[PASS] dataScope=5 returns only own actor rows"
} else {
  Write-Host "[FAIL] dataScope=5 leaked $nonSelfRows non-self rows"
}

Write-Host ""
Write-Host "=== cleanup: delete ds_user and ds_test_deptonly role ==="
$delU = Invoke-RestMethod -Uri "http://localhost:9080/system/user/list?userName=ds_user" -Method Get -Headers (Headers $adminTk)
foreach ($u in $delU.rows) { Invoke-RestMethod -Uri "http://localhost:9080/system/user/$($u.userId)" -Method Delete -Headers (Headers $adminTk) | Out-Null }
Invoke-RestMethod -Uri "http://localhost:9080/system/role/$newRoleId" -Method Delete -Headers (Headers $adminTk) | Out-Null
Write-Host "cleanup done"
