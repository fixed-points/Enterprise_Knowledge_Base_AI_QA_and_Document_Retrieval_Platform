$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$aiDir = Join-Path $projectRoot "ai-service"

. (Join-Path $scriptDir "set-ark-env.ps1")

$existing = Get-NetTCPConnection -State Listen -LocalPort 8000 -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "AI service is already running on port 8000." -ForegroundColor Yellow
    return
}

Set-Location $aiDir
$env:PYTHONUTF8 = "1"
py -m uvicorn app.main:app --host 0.0.0.0 --port 8000
