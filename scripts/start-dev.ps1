param(
    [switch]$WithInfra,
    [switch]$UseQuartzJdbc
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if ($WithInfra) {
    docker compose up -d mysql redis
}

$profiles = "druid"
if ($UseQuartzJdbc) {
    $profiles = "$profiles,quartz-jdbc"
}

$backend = Start-Process powershell -PassThru -WorkingDirectory "$root\backend" -ArgumentList @(
    "-NoExit",
    "-Command",
    "`$env:SPRING_PROFILES_ACTIVE='$profiles'; mvn spring-boot:run -pl scaffold-admin -am"
)

$frontend = Start-Process powershell -PassThru -WorkingDirectory "$root\frontend" -ArgumentList @(
    "-NoExit",
    "-Command",
    "npm run dev"
)

Write-Host "Backend terminal PID: $($backend.Id)"
Write-Host "Frontend terminal PID: $($frontend.Id)"
