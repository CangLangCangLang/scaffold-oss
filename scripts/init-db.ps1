param(
    [switch]$InfraOnly
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

docker compose up -d mysql redis

if (-not $InfraOnly) {
    Write-Host "Starting backend once so Liquibase can initialize or validate the schema..."
    docker compose up --build backend
}
