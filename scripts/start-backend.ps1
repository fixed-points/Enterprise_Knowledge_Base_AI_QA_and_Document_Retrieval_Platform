$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$backendDir = Join-Path $projectRoot "backend"
$jarPath = Join-Path $backendDir "target\knowledgebase-backend-1.0.0.jar"

$existing = Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Backend service is already running on port 8081." -ForegroundColor Yellow
    return
}

Set-Location $backendDir
if (-not (Test-Path $jarPath)) {
    Write-Host "Backend jar not found. Building package first..." -ForegroundColor Cyan
    mvn -q -DskipTests package
}

& java "-Dfile.encoding=UTF-8" "-jar" $jarPath "--server.port=8081"
