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

# Helper to test TCP port availability silently and rapidly without Test-NetConnection overhead
function Test-PortListening([string]$HostName = "127.0.0.1", [int]$Port = 5432, [int]$TimeoutMs = 1000) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $ar = $tcp.BeginConnect($HostName, $Port, $null, $null)
        $success = $ar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        if ($success) {
            $tcp.EndConnect($ar)
            $tcp.Close()
            return $true
        } else {
            $tcp.Close()
            return $false
        }
    } catch {
        return $false
    }
}

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

$pgListening = Test-PortListening -HostName "127.0.0.1" -Port 5432 -TimeoutMs 1000

if (-not $pgListening) {
    Write-Host "  PostgreSQL is not listening on port 5432. Attempting auto-detection & auto-start..." -ForegroundColor Yellow
    
    # 2a. Attempt starting Windows Service if installed
    $pgSvc = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object { $_.Status -ne "Running" } | Select-Object -First 1
    if ($pgSvc) {
        Write-Host "  Found Windows Service '$($pgSvc.Name)'. Starting service..." -ForegroundColor Gray
        Start-Service -Name $pgSvc.Name -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
    
    # 2b. Attempt starting standalone / portable binary if service was not active
    if (-not (Test-PortListening -HostName "127.0.0.1" -Port 5432 -TimeoutMs 500)) {
        $userPgBin = Join-Path $env:USERPROFILE "pgsql\bin\postgres.exe"
        $userPgData = Join-Path $env:USERPROFILE "pgsql\data"
        
        $candidateExe = $null
        $candidateData = $null
        
        if (Test-Path $userPgBin -ErrorAction SilentlyContinue) {
            $candidateExe = $userPgBin
            $candidateData = $userPgData
        } else {
            $searchPaths = @(
                "C:\Program Files\PostgreSQL\17\bin\postgres.exe",
                "C:\Program Files\PostgreSQL\16\bin\postgres.exe",
                "C:\Program Files\PostgreSQL\15\bin\postgres.exe",
                "C:\Program Files\PostgreSQL\14\bin\postgres.exe"
            )
            foreach ($p in $searchPaths) {
                if (Test-Path $p -ErrorAction SilentlyContinue) {
                    $candidateExe = $p
                    $parent = Split-Path (Split-Path $p -Parent) -Parent
                    $candidateData = Join-Path $parent "data"
                    break
                }
            }
        }
        
        if (-not $candidateExe) {
            $cmd = Get-Command postgres -ErrorAction SilentlyContinue
            if ($cmd) {
                $candidateExe = $cmd.Source
                $candidateData = Join-Path (Split-Path (Split-Path $candidateExe -Parent) -Parent) "data"
            }
        }
        
        if ($candidateExe -and (Test-Path $candidateExe -ErrorAction SilentlyContinue)) {
            Write-Host "  Found PostgreSQL executable: $candidateExe" -ForegroundColor Cyan
            if ($candidateData -and (Test-Path $candidateData -ErrorAction SilentlyContinue)) {
                Write-Host "  Data cluster: $candidateData" -ForegroundColor Gray
                Start-Process -FilePath $candidateExe -ArgumentList "-D `"$candidateData`"" -WindowStyle Hidden
            } else {
                Start-Process -FilePath $candidateExe -WindowStyle Hidden
            }
            
            # Poll up to 10 seconds for port 5432 to become available
            Write-Host "  Waiting for PostgreSQL daemon to accept connections..." -ForegroundColor Gray
            for ($s = 0; $s -lt 10; $s++) {
                Start-Sleep -Seconds 1
                if (Test-PortListening -HostName "127.0.0.1" -Port 5432 -TimeoutMs 500) {
                    $pgListening = $true
                    break
                }
            }
        }
    } else {
        $pgListening = $true
    }
    
    $pgListening = Test-PortListening -HostName "127.0.0.1" -Port 5432 -TimeoutMs 1000
}

if ($pgListening) {
    # Brief pause to ensure PostgreSQL transitions from socket-bind to accepting SQL connections
    Start-Sleep -Seconds 2
    Write-Host "  PostgreSQL is ACTIVE and accepting connections on port 5432." -ForegroundColor Green
    
    # Safely verify / create database cybertrace_db if createdb is available
    $createdbCandidates = @(
        (Join-Path $env:USERPROFILE "pgsql\bin\createdb.exe"),
        "C:\Program Files\PostgreSQL\17\bin\createdb.exe",
        "C:\Program Files\PostgreSQL\16\bin\createdb.exe",
        "C:\Program Files\PostgreSQL\15\bin\createdb.exe"
    )
    $createdbExe = $null
    foreach ($cand in $createdbCandidates) {
        if (Test-Path $cand -ErrorAction SilentlyContinue) {
            $createdbExe = $cand
            break
        }
    }
    if (-not $createdbExe) {
        $cCmd = Get-Command createdb -ErrorAction SilentlyContinue
        if ($cCmd) { $createdbExe = $cCmd.Source }
    }
    
    if ($createdbExe) {
        $dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" }
        $dbPass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }
        
        $cpsi = New-Object System.Diagnostics.ProcessStartInfo
        $cpsi.FileName = $createdbExe
        $cpsi.Arguments = "-U $dbUser -h 127.0.0.1 -p 5432 cybertrace_db"
        $cpsi.EnvironmentVariables["PGPASSWORD"] = $dbPass
        $cpsi.RedirectStandardError = $true
        $cpsi.RedirectStandardOutput = $true
        $cpsi.UseShellExecute = $false
        $cpsi.CreateNoWindow = $true
        
        try {
            $cproc = [System.Diagnostics.Process]::Start($cpsi)
            $null = $cproc.WaitForExit(6000)
            $cerr = $cproc.StandardError.ReadToEnd()
            if ($cproc.ExitCode -eq 0) {
                Write-Host "  Database 'cybertrace_db' initialized successfully." -ForegroundColor Green
            } elseif ($cerr -match "already exists") {
                Write-Host "  Database 'cybertrace_db' verified." -ForegroundColor Gray
            }
        } catch {}
    }
} else {
    Write-Host "CRITICAL: PostgreSQL is NOT listening on localhost:5432." -ForegroundColor Red
    Write-Host "The CyberTrace platform requires PostgreSQL to store cases, evidence, and audit logs." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please start PostgreSQL manually before running this launcher:" -ForegroundColor Cyan
    Write-Host "  Option 1 (User / Portable):" -ForegroundColor White
    Write-Host "    & `"$env:USERPROFILE\pgsql\bin\postgres.exe`" -D `"$env:USERPROFILE\pgsql\data`"" -ForegroundColor Gray
    Write-Host "  Option 2 (Windows Service):" -ForegroundColor White
    Write-Host "    Start-Service postgresql*" -ForegroundColor Gray
    Write-Host "  Option 3 (Verify Database Exists):" -ForegroundColor White
    Write-Host "    createdb -U postgres cybertrace_db" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

# Helper to check and clear port
function Clear-PortIfProjectProcess([int]$Port, [string]$ServiceName) {
    $conns = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    if ($conns) {
        $procIds = $conns | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $procIds) {
            if ($procId -gt 4) {
                $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($proc -and $proc.ProcessName -match 'java|javaw') {
                    Write-Host "  Port $Port occupied by existing Java process (PID $procId). Stopping cleanly..." -ForegroundColor Yellow
                    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                    try { $proc.WaitForExit(3000) } catch {}
                } elseif ($proc) {
                    Write-Warning "  Port $Port is occupied by PID $procId ($($proc.ProcessName)). Service $ServiceName may conflict."
                }
            }
        }
        for ($wait = 0; $wait -lt 8; $wait++) {
            $remaining = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
            if (-not $remaining) { break }
            Start-Sleep -Milliseconds 500
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
$m1Jar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-app.jar"
if (-not (Test-Path $m1Jar)) {
    $m1LegacyJar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-1.0.0-SNAPSHOT.jar"
    if (Test-Path $m1LegacyJar) { $m1Jar = $m1LegacyJar }
}
$m2Jar = Join-Path $ProjectRoot "cybertrace-m2\target\cybertrace-m2-timeline-engine-1.0.0-SNAPSHOT.jar"
$m3Jar = Join-Path $ProjectRoot "cybertrace-m3\target\cybertrace-m3-analysis-engine-1.0.0-SNAPSHOT.jar"
$mvnw = Join-Path $ProjectRoot "cybertrace-m1\mvnw.cmd"

if ($BuildJars -or (-not (Test-Path $m1Jar)) -or (-not (Test-Path $m2Jar)) -or (-not (Test-Path $m3Jar))) {
    Write-Host "`nBuilding application modules with Maven Wrapper..." -ForegroundColor Yellow
    & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m1\pom.xml") package -DskipTests
    if (Test-Path (Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-app.jar")) {
        $m1Jar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-app.jar"
    }
    & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m2\pom.xml") package -DskipTests
    & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m3\pom.xml") package -DskipTests
}

# Prepare logs directory under target/ to avoid git pollution
$logDir = Join-Path $ProjectRoot "target\logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}
$m1Log = Join-Path $logDir "m1-backend.log"
$m1Err = Join-Path $logDir "m1-backend-err.log"
$m2Log = Join-Path $logDir "m2-timeline.log"
$m2Err = Join-Path $logDir "m2-timeline-err.log"
$m3Log = Join-Path $logDir "m3-analysis.log"
$m3Err = Join-Path $logDir "m3-analysis-err.log"

# 4. Start Backend Microservices in Required Order: PostgreSQL -> M1 -> M2 -> M3 -> JavaFX
Write-Host "`n[4/5] Starting Backend Microservices (Order: M1 -> M2 -> M3)..." -ForegroundColor Yellow

# Start M1 (Backend on 8080)
Write-Host "  [4.1] Launching M1 Backend on port 8080..." -ForegroundColor Gray
$m1Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m1Jar`" --spring.profiles.active=dev" `
    -WorkingDirectory $ProjectRoot `
    -RedirectStandardOutput $m1Log `
    -RedirectStandardError $m1Err `
    -PassThru -WindowStyle Hidden

# Wait for M1 readiness
Write-Host "  Waiting for M1 Backend to initialize..." -ForegroundColor Gray
$m1Ready = $false
for ($i = 0; $i -lt 35; $i++) {
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
    Write-Host "  WARNING: M1 Backend did not report UP within 35s." -ForegroundColor Yellow
    if (Test-Path $m1Err) {
        Write-Host "  Recent error log from M1 Backend ($m1Err):" -ForegroundColor Gray
        Get-Content $m1Err -Tail 15 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
    }
}

# Start M2 (Timeline Engine on 8081)
Write-Host "  [4.2] Launching M2 Timeline Engine on port 8081..." -ForegroundColor Gray
$m2Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m2Jar`"" `
    -WorkingDirectory $ProjectRoot `
    -RedirectStandardOutput $m2Log `
    -RedirectStandardError $m2Err `
    -PassThru -WindowStyle Hidden

# Start M3 (Analysis Engine on 8082)
Write-Host "  [4.3] Launching M3 Analysis Engine on port 8082..." -ForegroundColor Gray
$m3Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m3Jar`"" `
    -WorkingDirectory $ProjectRoot `
    -RedirectStandardOutput $m3Log `
    -RedirectStandardError $m3Err `
    -PassThru -WindowStyle Hidden

# Wait for M2 & M3 readiness
Write-Host "  Waiting for M2 and M3 to initialize..." -ForegroundColor Gray
$m2Ready = $false
$m3Ready = $false
for ($j = 0; $j -lt 15; $j++) {
    Start-Sleep -Seconds 1
    if (-not $m2Ready -and (Test-PortListening -HostName "127.0.0.1" -Port 8081 -TimeoutMs 500)) {
        $m2Ready = $true
    }
    if (-not $m3Ready -and (Test-PortListening -HostName "127.0.0.1" -Port 8082 -TimeoutMs 500)) {
        $m3Ready = $true
    }
    if ($m2Ready -and $m3Ready) {
        break
    }
}

if ($m2Ready) {
    Write-Host "  M2 Timeline Engine: ONLINE (Port 8081)" -ForegroundColor Green
} else {
    Write-Host "  M2 Timeline Engine: Initializing (Port 8081)" -ForegroundColor Yellow
}

if ($m3Ready) {
    Write-Host "  M3 Analysis Engine: ONLINE (Port 8082)" -ForegroundColor Green
} else {
    Write-Host "  M3 Analysis Engine: Initializing (Port 8082)" -ForegroundColor Yellow
}

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
