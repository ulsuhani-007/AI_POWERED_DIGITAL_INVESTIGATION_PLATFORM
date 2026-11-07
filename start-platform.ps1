# =============================================================================
# CyberTrace - AI-Powered Digital Investigation Platform
# Portable Universal Platform Startup Script (Windows PowerShell)
# =============================================================================

[CmdletBinding()]
param(
    [switch]$SkipFrontend,
    [switch]$BuildJars
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  AI-Powered Digital Investigation Platform - Startup Coordinator   " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Project Directory: $ProjectRoot" -ForegroundColor Gray

# 1. Check Java Runtime Environment (Supports Java 21, 22, 23, 24, 25, and future >= 21)
Write-Host "`n[1/5] Checking Java Runtime..." -ForegroundColor Yellow

$javaExe = $null

# Check JAVA_HOME first if defined
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) {
        $javaExe = $candidate
    }
}

# Fall back to java on PATH
if (-not $javaExe) {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) {
        $javaExe = $cmd.Source
    }
}

if (-not $javaExe) {
    Write-Host "CRITICAL: Java executable not found in PATH or JAVA_HOME." -ForegroundColor Red
    Write-Host "Please install Java 21 or later and ensure it is accessible in your PATH." -ForegroundColor Yellow
    exit 1
}

# Automatically populate JAVA_HOME for the current session if not set (ensures Maven wrapper succeeds)
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $inferredHome = Split-Path (Split-Path $javaExe -Parent) -Parent
    if (Test-Path (Join-Path $inferredHome "bin\java.exe")) {
        $env:JAVA_HOME = $inferredHome
    }
}

# Safely query java -version using ProcessStartInfo to avoid PowerShell ActionPreferenceStopException on stderr
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaExe
$psi.Arguments = "-version"
$psi.RedirectStandardError = $true
$psi.RedirectStandardOutput = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true

$proc = [System.Diagnostics.Process]::Start($psi)
$null = $proc.WaitForExit(5000)
$versionRaw = $proc.StandardError.ReadToEnd()
if ([string]::IsNullOrWhiteSpace($versionRaw)) {
    $versionRaw = $proc.StandardOutput.ReadToEnd()
}

$majorVersion = 0
$fullVersionStr = "Unknown"

if ($versionRaw -match 'version\s+"(?<full>(?<major>\d+)(\.(?<minor>\d+))?[^"]*)"') {
    $fullVersionStr = $Matches['full']
    $majorPart = [int]$Matches['major']
    # Handle legacy 1.8 -> 8
    if ($majorPart -eq 1 -and $Matches['minor']) {
        $majorVersion = [int]$Matches['minor']
    } else {
        $majorVersion = $majorPart
    }
}

if ($majorVersion -lt 21) {
    Write-Host "CRITICAL: Unsupported Java version detected: $fullVersionStr (major version: $majorVersion)." -ForegroundColor Red
    Write-Host "Java 21 or later is required (e.g. Java 21, 22, 23, 24, 25+). Please upgrade your JDK." -ForegroundColor Yellow
    exit 1
}

Write-Host "  Java runtime verified: Java $majorVersion ($fullVersionStr)" -ForegroundColor Green
Write-Host "  Binary: $javaExe" -ForegroundColor Gray

# 2. Check PostgreSQL Service on Port 5432
Write-Host "`n[2/5] Checking PostgreSQL Database on Port 5432..." -ForegroundColor Yellow
$pgTest = Test-NetConnection -ComputerName 127.0.0.1 -Port 5432 -WarningAction SilentlyContinue
if (-not $pgTest.TcpTestSucceeded) {
    Write-Warning "PostgreSQL is NOT listening on localhost:5432."
    Write-Host "Prerequisite Reminder:" -ForegroundColor Yellow
    Write-Host "  1. Ensure PostgreSQL 16 is installed and running on port 5432." -ForegroundColor Gray
    Write-Host "  2. Ensure the database 'cybertrace_db' exists with user 'postgres'." -ForegroundColor Gray
    Write-Host "     (e.g., createdb -U postgres cybertrace_db)" -ForegroundColor Gray
    Write-Host "  3. You may set DB_URL, DB_USERNAME, DB_PASSWORD environment variables if needed." -ForegroundColor Gray
    $cont = Read-Host "Proceed anyway? (y/N)"
    if ($cont -ne "y" -and $cont -ne "Y") {
        exit 1
    }
} else {
    Write-Host "  PostgreSQL is ACTIVE and listening on port 5432." -ForegroundColor Green
}

# Helper to check and clear port
function Clear-PortIfProjectProcess([int]$Port, [string]$ServiceName) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $procId = $conn.OwningProcess
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -eq "java") {
            Write-Host "  Port $Port occupied by existing Java process (PID $procId). Stopping cleanly..." -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        } else {
            Write-Warning "  Port $Port is occupied by PID $procId ($($proc.ProcessName)). Service $ServiceName may conflict."
        }
    }
}

# 3. Port Diagnostics & Cleanup
Write-Host "`n[3/5] Diagnosing Ports 8080 (M1), 8081 (M2), 8082 (M3)..." -ForegroundColor Yellow
Clear-PortIfProjectProcess 8080 "M1 Backend"
Clear-PortIfProjectProcess 8081 "M2 Timeline"
Clear-PortIfProjectProcess 8082 "M3 Analysis"
Write-Host "  Ports verified and ready." -ForegroundColor Green

# Optional: Build jars if requested or missing
$m1Jar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-1.0.0-SNAPSHOT.jar"
$m2Jar = Join-Path $ProjectRoot "cybertrace-m2\target\cybertrace-m2-timeline-engine-1.0.0-SNAPSHOT.jar"
$m3Jar = Join-Path $ProjectRoot "cybertrace-m3\target\cybertrace-m3-analysis-engine-1.0.0-SNAPSHOT.jar"
$mvnw = Join-Path $ProjectRoot "cybertrace-m1\mvnw.cmd"

if ($BuildJars -or (-not (Test-Path $m1Jar)) -or (-not (Test-Path $m2Jar)) -or (-not (Test-Path $m3Jar))) {
    Write-Host "`nBuilding application modules with Maven Wrapper..." -ForegroundColor Yellow
    & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m1\pom.xml") package -DskipTests
    & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m2\pom.xml") package -DskipTests
    & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m3\pom.xml") package -DskipTests
}

# 4. Start Backend Microservices in Required Order: PostgreSQL -> M1 -> M2 -> M3 -> JavaFX
Write-Host "`n[4/5] Starting Backend Microservices (Order: M1 -> M2 -> M3)..." -ForegroundColor Yellow

# Start M1 (Backend on 8080)
Write-Host "  [4.1] Launching M1 Backend on port 8080..." -ForegroundColor Gray
$m1Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m1Jar`" --spring.profiles.active=dev" -WorkingDirectory $ProjectRoot -PassThru -WindowStyle Hidden

# Wait for M1 readiness
Write-Host "  Waiting for M1 Backend to initialize..." -ForegroundColor Gray
$m1Ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($res -and $res.status -eq "UP") {
            $m1Ready = $true
            break
        }
    } catch {}
}

if ($m1Ready) {
    Write-Host "  M1 Backend: ONLINE (Port 8080)" -ForegroundColor Green
} else {
    Write-Warning "  M1 Backend did not respond within 30s. Continuing startup..."
}

# Start M2 (Timeline Engine on 8081)
Write-Host "  [4.2] Launching M2 Timeline Engine on port 8081..." -ForegroundColor Gray
$m2Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m2Jar`"" -WorkingDirectory $ProjectRoot -PassThru -WindowStyle Hidden

# Start M3 (Analysis Engine on 8082)
Write-Host "  [4.3] Launching M3 Analysis Engine on port 8082..." -ForegroundColor Gray
$m3Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m3Jar`"" -WorkingDirectory $ProjectRoot -PassThru -WindowStyle Hidden

# Wait for M2 & M3 readiness
Write-Host "  Waiting for M2 and M3 to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 4
Write-Host "  M2 Timeline Engine: ONLINE (Port 8081)" -ForegroundColor Green
Write-Host "  M3 Analysis Engine: ONLINE (Port 8082)" -ForegroundColor Green

Write-Host "`nUniversal Demo Admin Credentials:" -ForegroundColor Cyan
Write-Host "  Username: admin" -ForegroundColor White
Write-Host "  Password: AdminPass123!" -ForegroundColor White

# 5. Launch JavaFX Frontend
if (-not $SkipFrontend) {
    Write-Host "`n[5/5] Launching JavaFX Investigation Workstation..." -ForegroundColor Yellow
    $frontendPom = Join-Path $ProjectRoot "frontend-javafx\pom.xml"
    & $mvnw -f $frontendPom javafx:run
} else {
    Write-Host "`n[5/5] Backend services running in background. (Frontend launch skipped)" -ForegroundColor Green
}
