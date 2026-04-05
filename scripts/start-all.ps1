$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Wait-HttpReady {
    param(
        [string]$Uri,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Uri $Uri -TimeoutSec 5 | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $scriptDir "start-ai.ps1")
)

Start-Sleep -Seconds 2

Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $scriptDir "start-backend.ps1")
)

Start-Sleep -Seconds 2

Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $scriptDir "start-frontend.ps1")
)

$aiReady = Wait-HttpReady -Uri "http://127.0.0.1:8000/api/health" -TimeoutSeconds 180
$backendReady = Wait-HttpReady -Uri "http://127.0.0.1:8081/api/documents" -TimeoutSeconds 150
$frontendReady = Wait-HttpReady -Uri "http://127.0.0.1:5173" -TimeoutSeconds 60

try {
    if ($aiReady -and $backendReady) {
        $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8081/api/documents/rebuild-indexes" -TimeoutSec 180
        $rebuiltCount = 0
        if ($response -and $response.data -ne $null) {
            $rebuiltCount = [int]$response.data
        }
        Write-Host "Knowledge index restore finished. Rebuilt documents: $rebuiltCount" -ForegroundColor Green
    } else {
        Write-Host "Automatic index restore was skipped because services were not ready in time." -ForegroundColor Yellow
    }
} catch {
    Write-Host "Automatic index restore was skipped. You can run scripts\\rebuild-indexes.ps1 later." -ForegroundColor Yellow
}

Write-Host "Requested startup for AI, backend, and frontend." -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:8081" -ForegroundColor Cyan
Write-Host "AI health: http://localhost:8000/api/health" -ForegroundColor Cyan
if (-not $frontendReady) {
    Write-Host "Frontend may still be starting. Refresh the page in a few seconds." -ForegroundColor Yellow
}
