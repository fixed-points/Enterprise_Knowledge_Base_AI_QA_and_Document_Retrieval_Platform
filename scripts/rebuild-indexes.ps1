$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 > $null

$rebuildUrl = "http://127.0.0.1:8081/api/documents/rebuild-indexes"

try {
    $response = Invoke-RestMethod -Method Post -Uri $rebuildUrl -TimeoutSec 120
    Write-Host "Knowledge index rebuild completed." -ForegroundColor Green
    $response | ConvertTo-Json -Depth 6
} catch {
    Write-Host "Failed to rebuild knowledge index." -ForegroundColor Red
    throw
}
