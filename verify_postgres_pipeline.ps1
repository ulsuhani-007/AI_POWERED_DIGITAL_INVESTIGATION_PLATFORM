# =============================================================================
# PostgreSQL Verification & Persistence-Across-Restart Test Script
# Tests: Native PostgreSQL 16 (:5432) + M1 (:8080) + M2 (:8081) + M3 (:8082)
# =============================================================================

$ErrorActionPreference = "Stop"

function Test-TcpPort {
    param([string]$HostName, [int]$Port)
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $iar = $tcp.BeginConnect($HostName, $Port, $null, $null)
        $wait = $iar.AsyncWaitHandle.WaitOne(800, $false)
        if ($wait) {
            $tcp.EndConnect($iar)
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

function Wait-ForPort {
    param([int]$Port, [string]$ServiceName, [int]$TimeoutSec = 45)
    $elapsed = 0
    Write-Host "Waiting for $ServiceName on port $Port..."
    while ($elapsed -lt $TimeoutSec) {
        if (Test-TcpPort -HostName "127.0.0.1" -Port $Port) {
            Write-Host "$ServiceName is UP and LISTENING on port $Port (in ${elapsed}s)"
            return $true
        }
        Start-Sleep -Seconds 1
        $elapsed++
    }
    Write-Error "TIMEOUT waiting for $ServiceName on port $Port"
    return $false
}

# Dynamic detection of psql and PostgreSQL
$psqlExe = $null
$cCmd = Get-Command psql -ErrorAction SilentlyContinue
if ($cCmd) {
    $psqlExe = $cCmd.Source
} else {
    $standardDirs = @()
    if ($env:ProgramFiles) { $standardDirs += (Join-Path $env:ProgramFiles "PostgreSQL") }
    if (${env:ProgramFiles(x86)}) { $standardDirs += (Join-Path ${env:ProgramFiles(x86)} "PostgreSQL") }
    foreach ($dir in $standardDirs) {
        if (Test-Path $dir -ErrorAction SilentlyContinue) {
            $subDirs = Get-ChildItem -Path $dir -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
            foreach ($sub in $subDirs) {
                $p = Join-Path $sub.FullName "bin\psql.exe"
                if (Test-Path $p -ErrorAction SilentlyContinue) {
                    $psqlExe = $p
                    break
                }
            }
            if ($psqlExe) { break }
        }
    }
}

Write-Host "=========================================================="
Write-Host "STEP 1: VERIFYING POSTGRESQL ON PORT 5432"
Write-Host "=========================================================="

if (-not (Test-TcpPort -HostName "127.0.0.1" -Port 5432)) {
    # Attempt to start Windows Service
    $pgSvc = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pgSvc) {
        Start-Service -Name $pgSvc.Name -ErrorAction SilentlyContinue
    }
    Wait-ForPort -Port 5432 -ServiceName "PostgreSQL Server" -TimeoutSec 15
} else {
    Write-Host "PostgreSQL is already active on port 5432."
}

# Clean public schema in cybertrace_db if psql is available
if ($psqlExe) {
    & $psqlExe -U postgres -h 127.0.0.1 -d cybertrace_db -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;" -ErrorAction SilentlyContinue
    Write-Host "PostgreSQL clean development database ready."
} else {
    Write-Host "PostgreSQL active; psql not in PATH, relying on Flyway migration."
}

Write-Host "`n=========================================================="
Write-Host "STEP 2: STARTING MICROSERVICES M3 (:8082), M2 (:8081), M1 (:8080)"
Write-Host "=========================================================="

# 1. Start M3 (:8082)
$m3Proc = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "cybertrace-m3\target\cybertrace-m3-analysis-engine-1.0.0-SNAPSHOT.jar" `
    -PassThru -NoNewWindow

# 2. Start M2 (:8081)
$m2Proc = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "cybertrace-m2\target\cybertrace-m2-timeline-engine-1.0.0-SNAPSHOT.jar", "--server.port=8081" `
    -PassThru -NoNewWindow

# 3. Start M1 (:8080) with PostgreSQL environment variables
$env:DB_URL = "jdbc:postgresql://localhost:5432/cybertrace_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
$env:SPRING_PROFILES_ACTIVE = "dev"

$m1Proc = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "cybertrace-m1\target\cybertrace-m1-backend-1.0.0-SNAPSHOT.jar" `
    -PassThru -NoNewWindow

try {
    Wait-ForPort -Port 8082 -ServiceName "M3 AI Engine" -TimeoutSec 45
    Wait-ForPort -Port 8081 -ServiceName "M2 Timeline Engine" -TimeoutSec 45
    Wait-ForPort -Port 8080 -ServiceName "M1 Backend (PostgreSQL)" -TimeoutSec 45

    Start-Sleep -Seconds 3

    Write-Host "`n=========================================================="
    Write-Host "STEP 3: VERIFY FLYWAY MIGRATION & POSTGRESQL TABLES"
    Write-Host "=========================================================="

    if ($psqlExe) {
        $tablesRaw = & $psqlExe -U postgres -h 127.0.0.1 -d cybertrace_db -t -c "\dt"
        $tablesOutput = $tablesRaw -join "`n"
        Write-Host "PostgreSQL Tables in cybertrace_db:"
        Write-Host $tablesOutput

        if ($tablesOutput -notmatch "cases" -or $tablesOutput -notmatch "evidence" -or $tablesOutput -notmatch "normalized_events" -or $tablesOutput -notmatch "flyway_schema_history") {
            throw "Flyway migration tables missing in PostgreSQL database"
        }
    } else {
        Write-Host "psql binary not found in standard paths; verifying via M1 REST API."
    }

    Write-Host "`n=========================================================="
    Write-Host "STEP 4: HEALTH AND READINESS & OPENAPI SWAGGER ENDPOINTS"
    Write-Host "=========================================================="

    $healthResp = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get
    Write-Host "Health Status: $($healthResp.status), Database: $($healthResp.database)"
    if ($healthResp.status -ne "UP" -or $healthResp.database -ne "CONNECTED") {
        throw "Health check failed: expected UP and CONNECTED"
    }

    $openApiResp = Invoke-RestMethod -Uri "http://localhost:8080/v3/api-docs" -Method Get
    Write-Host "OpenAPI 3 Doc Title: $($openApiResp.info.title), Version: $($openApiResp.info.version)"

    Write-Host "`n=========================================================="
    Write-Host "STEP 5: AUTHENTICATION API WITH POSTGRESQL PERSISTENCE"
    Write-Host "=========================================================="

    # Investigator login
    $loginBody = @{
        username = "investigator"
        password = "InvestigatorPass123!"
    } | ConvertTo-Json

    $authResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $loginBody

    Write-Host "Investigator Authenticated:"
    Write-Host "  User ID: $($authResp.userId)"
    Write-Host "  Role: $($authResp.role)"
    Write-Host "  Token: $($authResp.token.Substring(0, 16))..."

    $token = $authResp.token
    $headers = @{ "Authorization" = "Bearer $token" }

    Write-Host "`n=========================================================="
    Write-Host "STEP 5: CASE CREATION IN POSTGRESQL"
    Write-Host "=========================================================="

    $newCaseBody = @{
        caseNumber = "CASE-PG-2026-001"
        title = "Operation Persistent Sentinel"
        description = "Investigation backed by PostgreSQL production database with Flyway schema"
        priority = "CRITICAL"
        leadInvestigator = "Lead Examiner J. Vance"
    } | ConvertTo-Json

    $createdCase = Invoke-RestMethod -Uri "http://localhost:8080/api/cases" `
        -Method Post -Headers $headers -ContentType "application/json" -Body $newCaseBody

    $caseId = $createdCase.id
    Write-Host "Created Case ID: $caseId"
    Write-Host "Created Case Number: $($createdCase.caseNumber)"

    Write-Host "`n=========================================================="
    Write-Host "STEP 6: REAL EVIDENCE UPLOAD & FULL PIPELINE ORCHESTRATION"
    Write-Host "=========================================================="

    $csvPath = "cybertrace-m2\src\test\resources\samples\network_events.csv"
    Write-Host "Uploading evidence CSV to M1 (M1 -> M2 -> Postgres -> M3 -> Postgres)..."
    $uploadOutput = & curl.exe -s -H "Authorization: Bearer $token" `
        -F "file=@$csvPath" -F "description=Network capture for forensic analysis" `
        "http://localhost:8080/api/cases/$caseId/evidence"
    Write-Host "Upload response: $uploadOutput"

    $evidenceList = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/evidence" -Method Get -Headers $headers
    Write-Host "Evidence Status: $($evidenceList[0].status)"
    Write-Host "Evidence SHA-256: $($evidenceList[0].hashSha256)"
    Write-Host "Evidence Event Count: $($evidenceList[0].eventCount)"
    Write-Host "Evidence Uploaded By: $($evidenceList[0].uploadedBy)"

    if ($evidenceList[0].status -ne "PROCESSED") {
        throw "Evidence status was not PROCESSED"
    }
    if ($evidenceList[0].uploadedBy -ne "investigator") {
        throw "Evidence chain of custody: expected uploadedBy 'investigator', got '$($evidenceList[0].uploadedBy)'"
    }

    Write-Host "`n--- Testing Forensic Hardening: Duplicate Evidence Upload Rejection ---"
    $dupOutput = & curl.exe -s -w "%{http_code}" -H "Authorization: Bearer $token" `
        -F "file=@$csvPath" -F "description=Attempt duplicate upload" `
        "http://localhost:8080/api/cases/$caseId/evidence"
    $dupHttpCode = $dupOutput.Substring($dupOutput.Length - 3)
    Write-Host "Duplicate upload HTTP status code: $dupHttpCode"
    if ($dupHttpCode -ne "409") {
        throw "Duplicate evidence was NOT rejected with 409 Conflict! Got: $dupHttpCode"
    }
    Write-Host "[OK] Duplicate evidence correctly rejected with 409 Conflict"

    Write-Host "`n--- Testing Forensic Hardening: Forbidden Executable Upload Rejection ---"
    $fakeExePath = "$env:TEMP\danger_test.exe"
    Set-Content -Path $fakeExePath -Value "MZ...dummy binary"
    $exeOutput = & curl.exe -s -w "%{http_code}" -H "Authorization: Bearer $token" `
        -F "file=@$fakeExePath" -F "description=Attempt dangerous upload" `
        "http://localhost:8080/api/cases/$caseId/evidence"
    $exeHttpCode = $exeOutput.Substring($exeOutput.Length - 3)
    Remove-Item -Path $fakeExePath -Force -ErrorAction SilentlyContinue
    Write-Host "Executable upload HTTP status code: $exeHttpCode"
    if ($exeHttpCode -ne "400") {
        throw "Forbidden executable was NOT rejected with 400 Bad Request! Got: $exeHttpCode"
    }
    Write-Host "[OK] Executable file correctly rejected with 400 Bad Request"

    Write-Host "`n=========================================================="
    Write-Host "STEP 7: RETRIEVE EVENTS & ANALYSIS REPORT (M4 DTOs)"
    Write-Host "=========================================================="

    $events = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/events" -Method Get -Headers $headers
    Write-Host "Retrieved $($events.Count) normalized events from PostgreSQL"

    $analysis = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/analysis" -Method Get -Headers $headers
    Write-Host "Total Events in M3 Analysis: $($analysis.totalEvents)"
    Write-Host "Total Anomalies Flagged: $($analysis.totalAnomalies)"
    Write-Host "Findings Count: $($analysis.findings.Count)"
    Write-Host "AI Summary Excerpt: $($analysis.summary.Substring(0, [Math]::Min(120, $analysis.summary.Length)))..."

    Write-Host "`n=========================================================="
    Write-Host "STEP 8: PERSISTENCE ACROSS RESTART VERIFICATION (TASK 10)"
    Write-Host "=========================================================="

    Write-Host "Stopping M1 Backend process (PID: $($m1Proc.Id))..."
    Stop-Process -Id $m1Proc.Id -Force
    Start-Sleep -Seconds 3

    if (Test-TcpPort -HostName "127.0.0.1" -Port 8080) {
        throw "M1 did not stop cleanly"
    }
    Write-Host "M1 is OFFLINE. Port 8080 is closed."

    Write-Host "Restarting M1 Backend with PostgreSQL..."
    $m1Proc = Start-Process -FilePath "java" `
        -ArgumentList "-jar", "cybertrace-m1\target\cybertrace-m1-backend-1.0.0-SNAPSHOT.jar" `
        -PassThru -NoNewWindow

    Wait-ForPort -Port 8080 -ServiceName "M1 Backend (Restarted)" -TimeoutSec 45
    Start-Sleep -Seconds 3

    # Authenticate again after restart
    $reAuthResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $loginBody
    $reToken = $reAuthResp.token
    $reHeaders = @{ "Authorization" = "Bearer $reToken" }

    Write-Host "`nVerifying data persistence in restarted M1 from PostgreSQL:"

    # Verify Case still exists
    $reloadedCase = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId" -Method Get -Headers $reHeaders
    Write-Host "  [OK] Case persisted: ID=$($reloadedCase.id), Number=$($reloadedCase.caseNumber), Title=$($reloadedCase.title)"
    if ($reloadedCase.caseNumber -ne "CASE-PG-2026-001") {
        throw "Case persistence check failed!"
    }

    # Verify Evidence still exists
    $reloadedEvidence = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/evidence" -Method Get -Headers $reHeaders
    Write-Host "  [OK] Evidence persisted: File=$($reloadedEvidence[0].fileName), SHA-256=$($reloadedEvidence[0].hashSha256), Status=$($reloadedEvidence[0].status)"
    if ($reloadedEvidence[0].status -ne "PROCESSED") {
        throw "Evidence persistence check failed!"
    }

    # Verify Events still exist
    $reloadedEvents = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/events" -Method Get -Headers $reHeaders
    Write-Host "  [OK] Events persisted: $($reloadedEvents.Count) normalized events retrieved."
    if ($reloadedEvents.Count -ne 4) {
        throw "Events persistence check failed!"
    }

    # Verify Analysis still exists
    $reloadedAnalysis = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/analysis" -Method Get -Headers $reHeaders
    Write-Host "  [OK] Analysis persisted: TotalEvents=$($reloadedAnalysis.totalEvents), TotalAnomalies=$($reloadedAnalysis.totalAnomalies)"
    if ($reloadedAnalysis.totalEvents -ne 4) {
        throw "Analysis persistence check failed!"
    }

    if ($psqlExe) {
        Write-Host "`nPostgreSQL Direct Row Verification via psql:"
        $dbSummary = & $psqlExe -U postgres -h 127.0.0.1 -d cybertrace_db -c "SELECT (SELECT count(*) FROM cases) AS cases_count, (SELECT count(*) FROM evidence) AS evidence_count, (SELECT count(*) FROM normalized_events) AS events_count, (SELECT count(*) FROM analysis_reports) AS reports_count;"
        Write-Host ($dbSummary -join "`n")
    }

    Write-Host "`n=========================================================="
    Write-Host "ALL POSTGRESQL & PERSISTENCE-ACROSS-RESTART CHECKS PASSED!"
    Write-Host "=========================================================="

} finally {
    Write-Host "`nCleaning up background microservices..."
    if ($m1Proc -and -not $m1Proc.HasExited) { Stop-Process -Id $m1Proc.Id -Force }
    if ($m2Proc -and -not $m2Proc.HasExited) { Stop-Process -Id $m2Proc.Id -Force }
    if ($m3Proc -and -not $m3Proc.HasExited) { Stop-Process -Id $m3Proc.Id -Force }
    if ($pgProc -and -not $pgProc.HasExited) { Stop-Process -Id $pgProc.Id -Force }
    Write-Host "All processes cleanly shut down."
}
