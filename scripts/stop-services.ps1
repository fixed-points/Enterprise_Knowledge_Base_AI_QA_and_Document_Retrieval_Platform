$ErrorActionPreference = "SilentlyContinue"

$ports = 8000, 8081, 5173
$connections = Get-NetTCPConnection -State Listen -LocalPort $ports -ErrorAction SilentlyContinue
$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

if (-not $processIds) {
    Write-Host "No service processes found on ports 8000, 8081, or 5173." -ForegroundColor Yellow
    return
}

foreach ($processId in $processIds) {
    try {
        $process = Get-Process -Id $processId
        Stop-Process -Id $processId -Force
        Write-Host ("Stopped process: {0} ({1})" -f $process.ProcessName, $processId) -ForegroundColor Green
    } catch {
        Write-Host ("Failed to stop process: {0}" -f $processId) -ForegroundColor Red
    }
}
