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

# Determine project root dynamically from script location
$ProjectRoot = $PSScriptRoot
if (-not $ProjectRoot) {
    $ProjectRoot = (Get-Item -Path ".").FullName
}

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  CyberTrace Platform - Portable Universal Launcher                 " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Project Directory: $ProjectRoot" -ForegroundColor Gray

# Helper to test TCP port availability silently and rapidly
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

# Helper to test HTTP service health silently
function Test-HttpHealth([string]$Url, [int]$TimeoutSec = 2) {
    try {
        $res = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec $TimeoutSec -ErrorAction SilentlyContinue
        if ($res) {
            if ($res.status -eq "UP" -or $res -is [PSCustomObject] -or $res -is [hashtable]) {
                return $true
            }
        }
        return $false
    } catch {
        return $false
    }
}

# [1/5] Checking Java...
Write-Host "`n[1/5] Checking Java..." -ForegroundColor Yellow

$javaExe = $null

# 1. Check JAVA_HOME first if defined
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) {
        $javaExe = $candidate
    }
}

# 2. Fall back to java on PATH
if (-not $javaExe) {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) {
        $javaExe = $cmd.Source
    }
}

if (-not $javaExe) {
    Write-Host "CRITICAL: Java executable not found in PATH or JAVA_HOME." -ForegroundColor Red
    Write-Host "Java 21 or later is required. Please install Java and ensure JAVA_HOME or PATH is configured." -ForegroundColor Yellow
    exit 1
}

# Safely query java -version using ProcessStartInfo to avoid stderr stopping PowerShell
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
    if ($majorPart -eq 1 -and $Matches['minor']) {
        $majorVersion = [int]$Matches['minor']
    } else {
        $majorVersion = $majorPart
    }
}

if ($majorVersion -lt 21) {
    Write-Host "CRITICAL: Unsupported Java version detected: $fullVersionStr (major version: $majorVersion)." -ForegroundColor Red
    Write-Host "Java 21 or later is required. Please install Java and ensure JAVA_HOME or PATH is configured." -ForegroundColor Yellow
    exit 1
}

Write-Host "  Java runtime verified: Java $majorVersion ($fullVersionStr)" -ForegroundColor Green
Write-Host "  Binary: $javaExe" -ForegroundColor Gray

# Automatically populate JAVA_HOME for the current session if not set (ensures Maven wrapper succeeds)
if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $inferredHome = Split-Path (Split-Path $javaExe -Parent) -Parent
    if (Test-Path (Join-Path $inferredHome "bin\java.exe")) {
        $env:JAVA_HOME = $inferredHome
    }
}

# [2/5] Checking PostgreSQL...
Write-Host "`n[2/5] Checking PostgreSQL..." -ForegroundColor Yellow

$pgListening = Test-PortListening -HostName "127.0.0.1" -Port 5432 -TimeoutMs 1000

if (-not $pgListening) {
    Write-Host "  PostgreSQL is not listening on port 5432. Attempting dynamic auto-detection..." -ForegroundColor Yellow
    
    # 2a. Attempt starting Windows Service if installed
    $pgSvc = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object { $_.Status -ne "Running" } | Select-Object -First 1
    if ($pgSvc) {
        Write-Host "  Found Windows Service '$($pgSvc.Name)'. Attempting to start service..." -ForegroundColor Gray
        Start-Service -Name $pgSvc.Name -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
    
    # 2b. Attempt starting standalone binary from standard installation directories or PATH
    if (-not (Test-PortListening -HostName "127.0.0.1" -Port 5432 -TimeoutMs 500)) {
        $candidateExe = $null
        $candidateData = $null
        
        # Check PATH first
        $cmd = Get-Command postgres -ErrorAction SilentlyContinue
        if ($cmd) {
            $candidateExe = $cmd.Source
            $candidateData = Join-Path (Split-Path (Split-Path $candidateExe -Parent) -Parent) "data"
        }
        
        # Check standard Windows directories
        if (-not $candidateExe) {
            $standardDirs = @()
            if ($env:ProgramFiles) { $standardDirs += (Join-Path $env:ProgramFiles "PostgreSQL") }
            if (${env:ProgramFiles(x86)}) { $standardDirs += (Join-Path ${env:ProgramFiles(x86)} "PostgreSQL") }
            
            foreach ($dir in $standardDirs) {
                if (Test-Path $dir -ErrorAction SilentlyContinue) {
                    $subDirs = Get-ChildItem -Path $dir -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
                    foreach ($sub in $subDirs) {
                        $pExe = Join-Path $sub.FullName "bin\postgres.exe"
                        if (Test-Path $pExe -ErrorAction SilentlyContinue) {
                            $candidateExe = $pExe
                            $candidateData = Join-Path $sub.FullName "data"
                            break
                        }
                    }
                    if ($candidateExe) { break }
                }
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

$m1Profile = "dev"

if ($pgListening) {
    Start-Sleep -Seconds 1
    Write-Host "  PostgreSQL is ACTIVE and accepting connections on port 5432." -ForegroundColor Green
    $m1Profile = "dev"
    
    # Safely verify / create database cybertrace_db if createdb is available
    $createdbExe = $null
    $cCmd = Get-Command createdb -ErrorAction SilentlyContinue
    if ($cCmd) {
        $createdbExe = $cCmd.Source
    } else {
        $standardDirs = @()
        if ($env:ProgramFiles) { $standardDirs += (Join-Path $env:ProgramFiles "PostgreSQL") }
        if (${env:ProgramFiles(x86)}) { $standardDirs += (Join-Path ${env:ProgramFiles(x86)} "PostgreSQL") }
        foreach ($dir in $standardDirs) {
            if (Test-Path $dir -ErrorAction SilentlyContinue) {
                $subDirs = Get-ChildItem -Path $dir -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
                foreach ($sub in $subDirs) {
                    $cExe = Join-Path $sub.FullName "bin\createdb.exe"
                    if (Test-Path $cExe -ErrorAction SilentlyContinue) {
                        $createdbExe = $cExe
                        break
                    }
                }
                if ($createdbExe) { break }
            }
        }
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
    Write-Host "  PostgreSQL is not listening on localhost:5432." -ForegroundColor Yellow
    Write-Host "  [AUTO-FALLBACK] Activating Embedded Persistent Database Engine (H2 file-based storage: data/cybertrace_db)." -ForegroundColor Cyan
    Write-Host "  All case data, evidence, and AI findings will persist locally." -ForegroundColor Gray
    $m1Profile = "h2"
}

# Locate Maven Wrapper
$mvnw = Join-Path $ProjectRoot "mvnw.cmd"
if (-not (Test-Path $mvnw)) {
    $mvnw = Join-Path $ProjectRoot "cybertrace-m1\mvnw.cmd"
}

# Jars locations
$m1Jar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-app.jar"
if (-not (Test-Path $m1Jar)) {
    $m1LegacyJar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-1.0.0-SNAPSHOT.jar"
    if (Test-Path $m1LegacyJar) { $m1Jar = $m1LegacyJar }
}
$m2Jar = Join-Path $ProjectRoot "cybertrace-m2\target\cybertrace-m2-timeline-engine-1.0.0-SNAPSHOT.jar"
$m3Jar = Join-Path $ProjectRoot "cybertrace-m3\target\cybertrace-m3-analysis-engine-1.0.0-SNAPSHOT.jar"
$frontendJar = Join-Path $ProjectRoot "frontend-javafx\target\frontend-javafx-1.0.0-SNAPSHOT.jar"

# Build jars if requested or missing
if ($BuildJars -or (-not (Test-Path $m1Jar)) -or (-not (Test-Path $m2Jar)) -or (-not (Test-Path $m3Jar)) -or (-not (Test-Path $frontendJar))) {
    Write-Host "`nBuilding application modules with Maven Wrapper (first run)..." -ForegroundColor Yellow
    if (-not (Test-Path $m1Jar)) {
        Write-Host "  Packaging M1 Backend..." -ForegroundColor Gray
        & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m1\pom.xml") package -DskipTests
        if (Test-Path (Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-app.jar")) {
            $m1Jar = Join-Path $ProjectRoot "cybertrace-m1\target\cybertrace-m1-backend-app.jar"
        }
    }
    if (-not (Test-Path $m2Jar)) {
        Write-Host "  Packaging M2 Timeline Engine..." -ForegroundColor Gray
        & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m2\pom.xml") package -DskipTests
    }
    if (-not (Test-Path $m3Jar)) {
        Write-Host "  Packaging M3 Analysis Engine..." -ForegroundColor Gray
        & $mvnw -f (Join-Path $ProjectRoot "cybertrace-m3\pom.xml") package -DskipTests
    }
    if (-not (Test-Path $frontendJar)) {
        Write-Host "  Packaging JavaFX Desktop Workstation..." -ForegroundColor Gray
        & $mvnw -f (Join-Path $ProjectRoot "frontend-javafx\pom.xml") package -DskipTests
    }
}

# Prepare logs and data directories
$logDir = Join-Path $ProjectRoot "target\logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}
$dataDir = Join-Path $ProjectRoot "data"
if (-not (Test-Path $dataDir)) {
    New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
}
$m1Log = Join-Path $logDir "m1-backend.log"
$m1Err = Join-Path $logDir "m1-backend-err.log"
$m2Log = Join-Path $logDir "m2-timeline.log"
$m2Err = Join-Path $logDir "m2-timeline-err.log"
$m3Log = Join-Path $logDir "m3-analysis.log"
$m3Err = Join-Path $logDir "m3-analysis-err.log"

# [3/5] Starting M1...
Write-Host "`n[3/5] Starting M1..." -ForegroundColor Yellow

$m1Running = $false
if (Test-PortListening -HostName "127.0.0.1" -Port 8080 -TimeoutMs 500) {
    if (Test-HttpHealth -Url "http://localhost:8080/api/health") {
        Write-Host "  M1 Backend: ONLINE (Port 8080 - Reusing existing healthy instance)" -ForegroundColor Green
        $m1Running = $true
    } else {
        # Check process occupying port 8080
        $conns = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
        $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($p in $pids) {
            if ($p -gt 4) {
                $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
                if ($proc -and $proc.ProcessName -match 'java|javaw') {
                    Write-Host "  Port 8080 occupied by unresponsive Java process (PID $p). Stopping cleanly..." -ForegroundColor Yellow
                    Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                    Start-Sleep -Seconds 2
                } else {
                    Write-Host "CRITICAL: Port 8080 is occupied by an unrelated application ($($proc.ProcessName), PID $p)." -ForegroundColor Red
                    Write-Host "Please free port 8080 or stop PID $p before starting CyberTrace M1." -ForegroundColor Yellow
                    exit 1
                }
            }
        }
    }
}

if (-not $m1Running) {
    Write-Host "  Launching M1 Backend on port 8080 (Profile: $m1Profile)..." -ForegroundColor Gray
    $m1Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m1Jar`" --spring.profiles.active=$m1Profile" `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $m1Log `
        -RedirectStandardError $m1Err `
        -PassThru -WindowStyle Hidden

    Write-Host "  Waiting for M1 Backend to initialize..." -ForegroundColor Gray
    $m1Ready = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        if (Test-HttpHealth -Url "http://localhost:8080/api/health") {
            $m1Ready = $true
            break
        }
    }

    if ($m1Ready) {
        Write-Host "  M1 Backend: ONLINE (Port 8080)" -ForegroundColor Green
    } else {
        Write-Host "  WARNING: M1 Backend did not report UP within 40s." -ForegroundColor Yellow
        if (Test-Path $m1Err) {
            Write-Host "  Recent error log from M1 Backend ($m1Err):" -ForegroundColor Gray
            Get-Content $m1Err -Tail 15 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
        }
    }
}

# [4/5] Starting M2 and M3...
Write-Host "`n[4/5] Starting M2 and M3..." -ForegroundColor Yellow

# --- M2 on Port 8081 ---
$m2Running = $false
if (Test-PortListening -HostName "127.0.0.1" -Port 8081 -TimeoutMs 500) {
    $conns2 = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
    $pids2 = $conns2 | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($p in $pids2) {
        if ($p -gt 4) {
            $proc2 = Get-Process -Id $p -ErrorAction SilentlyContinue
            if ($proc2 -and $proc2.ProcessName -match 'java|javaw') {
                $m2Running = $true
            } else {
                Write-Host "CRITICAL: Port 8081 is occupied by an unrelated application ($($proc2.ProcessName), PID $p)." -ForegroundColor Red
                Write-Host "Please free port 8081 before starting CyberTrace M2." -ForegroundColor Yellow
                exit 1
            }
        }
    }
    if ($m2Running) {
        Write-Host "  M2 Timeline Engine: ONLINE (Port 8081 - Reusing existing instance)" -ForegroundColor Green
    }
}

if (-not $m2Running) {
    Write-Host "  Launching M2 Timeline Engine on port 8081..." -ForegroundColor Gray
    $m2Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m2Jar`"" `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $m2Log `
        -RedirectStandardError $m2Err `
        -PassThru -WindowStyle Hidden
}

# --- M3 on Port 8082 ---
$m3Running = $false
if (Test-PortListening -HostName "127.0.0.1" -Port 8082 -TimeoutMs 500) {
    $conns3 = Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue
    $pids3 = $conns3 | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($p in $pids3) {
        if ($p -gt 4) {
            $proc3 = Get-Process -Id $p -ErrorAction SilentlyContinue
            if ($proc3 -and $proc3.ProcessName -match 'java|javaw') {
                $m3Running = $true
            } else {
                Write-Host "CRITICAL: Port 8082 is occupied by an unrelated application ($($proc3.ProcessName), PID $p)." -ForegroundColor Red
                Write-Host "Please free port 8082 before starting CyberTrace M3." -ForegroundColor Yellow
                exit 1
            }
        }
    }
    if ($m3Running) {
        Write-Host "  M3 Analysis Engine: ONLINE (Port 8082 - Reusing existing instance)" -ForegroundColor Green
    }
}

if (-not $m3Running) {
    Write-Host "  Launching M3 Analysis Engine on port 8082..." -ForegroundColor Gray
    $m3Proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$m3Jar`"" `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $m3Log `
        -RedirectStandardError $m3Err `
        -PassThru -WindowStyle Hidden
}

# Wait for M2 & M3 if newly launched
if (-not $m2Running -or -not $m3Running) {
    Write-Host "  Waiting for M2 and M3 to initialize..." -ForegroundColor Gray
    for ($j = 0; $j -lt 20; $j++) {
        Start-Sleep -Seconds 1
        if (-not $m2Running -and (Test-PortListening -HostName "127.0.0.1" -Port 8081 -TimeoutMs 500)) {
            $m2Running = $true
        }
        if (-not $m3Running -and (Test-PortListening -HostName "127.0.0.1" -Port 8082 -TimeoutMs 500)) {
            $m3Running = $true
        }
        if ($m2Running -and $m3Running) { break }
    }
    if ($m2Running) { Write-Host "  M2 Timeline Engine: ONLINE (Port 8081)" -ForegroundColor Green }
    if ($m3Running) { Write-Host "  M3 Analysis Engine: ONLINE (Port 8082)" -ForegroundColor Green }
}

Write-Host "`nCyberTrace platform started successfully." -ForegroundColor Green
Write-Host "  M1: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  M2: http://localhost:8081" -ForegroundColor Cyan
Write-Host "  M3: http://localhost:8082" -ForegroundColor Cyan
Write-Host ""
Write-Host "Universal Demo Admin Credentials:" -ForegroundColor Cyan
Write-Host "  Username: admin" -ForegroundColor White
Write-Host "  Password: AdminPass123!" -ForegroundColor White

# [5/5] Starting JavaFX...
Write-Host "`n[5/5] Starting JavaFX..." -ForegroundColor Yellow
if (-not $SkipFrontend) {
    if (Test-Path $frontendJar) {
        Write-Host "  Launching JavaFX Investigation Workstation..." -ForegroundColor Green
        & $javaExe --enable-native-access=ALL-UNNAMED -jar $frontendJar
    } else {
        $frontendPom = Join-Path $ProjectRoot "frontend-javafx\pom.xml"
        & $mvnw -f $frontendPom javafx:run
    }
} else {
    Write-Host "  JavaFX launch skipped (-SkipFrontend). Backend services active in background." -ForegroundColor Green
}
