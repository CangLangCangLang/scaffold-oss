$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scripts = @(
  "verify-data-scope.ps1"
  "verify-observability.ps1"
  "verify-workflow-enhancements.ps1"
  "verify-cms.ps1"
  "verify-cms-workflow.ps1"
  "verify-file.ps1"
  "verify-form.ps1"
  "verify-report.ps1"
)

$results = @()
$logDir = Join-Path (Split-Path $PSScriptRoot -Parent) "..\verify-logs"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }

foreach ($s in $scripts) {
  $path = Join-Path $PSScriptRoot $s
  if (-not (Test-Path $path)) {
    Write-Host "[SKIP] $s 不存在"
    $results += [PSCustomObject]@{ Script = $s; Exit = "MISSING"; Sec = 0; Tail = "" }
    continue
  }
  Write-Host "===== $s ====="
  $logFile = Join-Path $logDir ($s -replace '\.ps1$','.log')
  $startedAt = Get-Date
  # 用 pwsh -File 启独立子进程，确保异常不传染本 runner
  $proc = Start-Process -FilePath "pwsh" `
    -ArgumentList @("-NoLogo","-NoProfile","-File", $path) `
    -RedirectStandardOutput $logFile `
    -RedirectStandardError ($logFile + ".err") `
    -PassThru -NoNewWindow -Wait
  $code = $proc.ExitCode
  $cost = ((Get-Date) - $startedAt).TotalSeconds
  $tail = (Get-Content $logFile -Encoding utf8 -Tail 4 -ErrorAction SilentlyContinue) -join " | "
  $results += [PSCustomObject]@{
    Script = $s
    Exit   = $code
    Sec    = [Math]::Round($cost, 1)
    Tail   = $tail
  }
  Write-Host ("  exit={0}  {1}s" -f $code, [Math]::Round($cost,1))
}

Write-Host ""
Write-Host "===== Summary ====="
$results | Format-Table -AutoSize
$pass = ($results | Where-Object { $_.Exit -eq 0 }).Count
$fail = ($results | Where-Object { $_.Exit -ne 0 -and $_.Exit -ne "MISSING" }).Count
$miss = ($results | Where-Object { $_.Exit -eq "MISSING" }).Count
Write-Host "Pass: $pass    Fail: $fail    Missing: $miss"

if ($fail -gt 0 -or $miss -gt 0) {
  exit 1
}

exit 0
