# =============================================================================
# CyberTrace - AI-Powered Digital Investigation Platform
# Stop & Port Cleanup Script (Windows PowerShell)
# =============================================================================

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  CyberTrace Platform - Stopping Background Services               " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

$ports = @(8080, 8081, 8082)
$found = 0

foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conns) {
        $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($pidToKill in $pids) {
            $proc = Get-Process -Id $pidToKill -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Host "  [OK] Terminating $($proc.ProcessName) on port $port (PID: $pidToKill)..." -ForegroundColor Yellow
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
                $found++
            }
        }
    }
}

if ($found -eq 0) {
    Write-Host "  No processes were running on ports 8080, 8081, or 8082." -ForegroundColor Green
} else {
    Write-Host "`nAll platform ports (8080, 8081, 8082) are now completely FREE." -ForegroundColor Green
}

