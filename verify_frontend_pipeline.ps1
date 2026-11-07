# End-to-end multi-service pipeline verification with Spring Security + M1 + M2 + M3

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

Write-Host "=========================================================="
Write-Host "STARTING MICROSERVICES M3 (:8082), M2 (:8081), M1 (:8080)"
Write-Host "=========================================================="

# 1. Start M3 (:8082)
$m3Proc = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "cybertrace-m3\target\cybertrace-m3-analysis-engine-1.0.0-SNAPSHOT.jar" `
    -PassThru -NoNewWindow

# 2. Start M2 (:8081)
$m2Proc = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "cybertrace-m2\target\cybertrace-m2-timeline-engine-1.0.0-SNAPSHOT.jar", "--server.port=8081" `
    -PassThru -NoNewWindow

# 3. Start M1 (:8080)
$m1Proc = Start-Process -FilePath "java" `
    -ArgumentList "-jar", "cybertrace-m1\target\cybertrace-m1-backend-1.0.0-SNAPSHOT.jar" `
    -PassThru -NoNewWindow

try {
    Wait-ForPort -Port 8082 -ServiceName "M3 AI Engine" -TimeoutSec 45
    Wait-ForPort -Port 8081 -ServiceName "M2 Timeline Engine" -TimeoutSec 45
    Wait-ForPort -Port 8080 -ServiceName "M1 Backend" -TimeoutSec 45

    Start-Sleep -Seconds 3

    Write-Host "`n=========================================================="
    Write-Host "PHASE 0: VERIFY SECURITY ENFORCEMENT ON UNAUTHENTICATED CALLS"
    Write-Host "=========================================================="

    # Attempt to access protected /api/cases without authentication
    try {
        $unauthResp = Invoke-RestMethod -Uri "http://localhost:8080/api/cases" -Method Get
        throw "Security failure: unauthenticated request to /api/cases returned 200 OK!"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Unauthenticated request correctly rejected with HTTP Status: $statusCode"
        if ($statusCode -ne 401) {
            throw "Expected HTTP 401 Unauthorized, got $statusCode"
        }
    }

    Write-Host "`n=========================================================="
    Write-Host "PHASE 1: AUTHENTICATION API (POST /api/auth/login)"
    Write-Host "=========================================================="

    # 1.1 Invalid credentials check
    try {
        $badLoginBody = @{
            username = "investigator"
            password = "WrongPassword999!"
        } | ConvertTo-Json
        $badLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
            -Method Post -ContentType "application/json" -Body $badLoginBody
        throw "Security failure: invalid password returned 200 OK!"
    } catch {
        $badStatus = $_.Exception.Response.StatusCode.value__
        Write-Host "Invalid password rejected with HTTP Status: $badStatus (Expected 401)"
        if ($badStatus -ne 401) {
            throw "Expected HTTP 401 Bad Credentials, got $badStatus"
        }
    }

    # 1.2 Investigator login
    $invLoginBody = @{
        username = "investigator"
        password = "InvestigatorPass123!"
    } | ConvertTo-Json
    $invAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $invLoginBody

    Write-Host "Investigator Authentication Succeeded!"
    Write-Host "  User ID: $($invAuth.userId)"
    Write-Host "  Username: $($invAuth.username)"
    Write-Host "  Role: $($invAuth.role)"
    Write-Host "  Token: $($invAuth.token.Substring(0, 16))..."
    Write-Host "  Expires At: $($invAuth.expiresAt)"

    if ($invAuth.role -ne "INVESTIGATOR" -or -not $invAuth.token) {
        throw "Invalid investigator authentication response"
    }

    $invToken = $invAuth.token
    $invHeaders = @{
        "Authorization" = "Bearer $invToken"
    }

    # 1.3 Administrator login
    $adminLoginBody = @{
        username = "admin"
        password = "AdminPass123!"
    } | ConvertTo-Json
    $adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
        -Method Post -ContentType "application/json" -Body $adminLoginBody

    Write-Host "`nAdministrator Authentication Succeeded!"
    Write-Host "  Username: $($adminAuth.username)"
    Write-Host "  Role: $($adminAuth.role)"
    $adminToken = $adminAuth.token
    $adminHeaders = @{
        "Authorization" = "Bearer $adminToken"
    }

    Write-Host "`n=========================================================="
    Write-Host "PHASE 2: ROLE-BASED AUTHORIZATION (ADMIN vs INVESTIGATOR)"
    Write-Host "=========================================================="

    # Investigator attempts to access Admin-only /api/users endpoint
    try {
        $forbiddenResp = Invoke-RestMethod -Uri "http://localhost:8080/api/users" `
            -Method Get -Headers $invHeaders
        throw "Authorization failure: investigator accessed admin user list!"
    } catch {
        $forbidStatus = $_.Exception.Response.StatusCode.value__
        Write-Host "Investigator blocked from /api/users with HTTP Status: $forbidStatus (Expected 403 Forbidden)"
        if ($forbidStatus -ne 403) {
            throw "Expected HTTP 403 Forbidden, got $forbidStatus"
        }
    }

    # Admin accesses /api/users
    $usersList = Invoke-RestMethod -Uri "http://localhost:8080/api/users" `
        -Method Get -Headers $adminHeaders
    Write-Host "Admin successfully retrieved user list. Count: $($usersList.Count)"
    foreach ($u in $usersList) {
        Write-Host "  User: $($u.username) | Role: $($u.role) | Enabled: $($u.enabled)"
    }

    # Admin creates new investigator account
    $newUserBody = @{
        username = "agent_smith"
        password = "MatrixPassword123!"
        role = "INVESTIGATOR"
    } | ConvertTo-Json
    $createdUser = Invoke-RestMethod -Uri "http://localhost:8080/api/users" `
        -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $newUserBody
    Write-Host "Admin successfully created user: $($createdUser.username) [ID: $($createdUser.id)]"

    Write-Host "`n=========================================================="
    Write-Host "PHASE 3: CASE MANAGEMENT (WITH INVESTIGATOR TOKEN)"
    Write-Host "=========================================================="

    # GET /api/cases with Bearer token
    $initialCases = Invoke-RestMethod -Uri "http://localhost:8080/api/cases" -Method Get -Headers $invHeaders
    Write-Host "Initial Cases in M1 DB: $($initialCases.Count)"

    # POST /api/cases
    $newCaseBody = @{
        caseNumber = "CASE-2026-LIVE"
        title = "Operation Falcon Sentinel"
        description = "Live investigation into unauthorized credential harvesting and data exfiltration"
        priority = "CRITICAL"
        leadInvestigator = "Agent J. Vance"
    } | ConvertTo-Json

    $createdCase = Invoke-RestMethod -Uri "http://localhost:8080/api/cases" `
        -Method Post `
        -Headers $invHeaders `
        -ContentType "application/json" `
        -Body $newCaseBody

    Write-Host "Created Case ID: $($createdCase.id)"
    Write-Host "Created Case Number: $($createdCase.caseNumber)"
    Write-Host "Created Case Title: $($createdCase.title)"
    Write-Host "Created Case Status: $($createdCase.status)"

    if (-not $createdCase.id) {
        throw "Failed to create case in M1"
    }
    $caseId = $createdCase.id

    Write-Host "`n=========================================================="
    Write-Host "PHASE 4: REAL EVIDENCE UPLOAD & PIPELINE ORCHESTRATION"
    Write-Host "=========================================================="

    $csvPath = "cybertrace-m2\src\test\resources\samples\network_events.csv"
    if (-not (Test-Path $csvPath)) {
        throw "Sample evidence file not found at $csvPath"
    }

    Write-Host "Uploading evidence CSV with Bearer token to M1 (M1 -> M2 -> DB -> M3 -> DB)..."
    $uploadOutput = & curl.exe -s -H "Authorization: Bearer $invToken" `
        -F "file=@$csvPath" -F "description=Falcon incident log file" `
        "http://localhost:8080/api/cases/$caseId/evidence"
    Write-Host "Upload response: $uploadOutput"

    # GET /api/cases/{id}/evidence
    $evidenceList = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/evidence" -Method Get -Headers $invHeaders
    Write-Host "Evidence Records Count: $($evidenceList.Count)"
    Write-Host "Evidence Status: $($evidenceList[0].status)"
    Write-Host "Evidence SHA-256: $($evidenceList[0].hashSha256)"
    Write-Host "Evidence Event Count: $($evidenceList[0].eventCount)"

    if ($evidenceList[0].status -ne "PROCESSED") {
        throw "Evidence status was not PROCESSED"
    }

    Write-Host "`n=========================================================="
    Write-Host "PHASE 5: RETRIEVE EVENTS & ANALYSIS REPORT (M4 DTO FORMAT)"
    Write-Host "=========================================================="

    $events = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/events" -Method Get -Headers $invHeaders
    Write-Host "Retrieved $($events.Count) normalized events from M1 database"
    Write-Host "First Event: ID=$($events[0].event_id), Type=$($events[0].event_type), User=$($events[0].user)"
    Write-Host "Last Event: ID=$($events[-1].event_id), Type=$($events[-1].event_type), Bytes=$($events[-1].bytes_transferred)"

    if ($events.Count -lt 4) {
        throw "Expected at least 4 normalized events"
    }

    $analysis = Invoke-RestMethod -Uri "http://localhost:8080/api/cases/$caseId/analysis" -Method Get -Headers $invHeaders
    Write-Host "Analysis Case ID: $($analysis.caseId)"
    Write-Host "Total Events Processed by M3: $($analysis.totalEvents)"
    Write-Host "Total Anomalies Detected by M3: $($analysis.totalAnomalies)"
    Write-Host "Findings Count: $($analysis.findings.Count)"
    Write-Host "`nAI Grounded Summary:"
    Write-Host $analysis.summary

    if ($analysis.findings.Count -gt 0) {
        Write-Host "`nDetected Findings Sample:"
        foreach ($f in $analysis.findings | Select-Object -First 3) {
            Write-Host "  - Event: $($f.event_id) | Method: $($f.detection_method) | Severity: $($f.severity) | Score: $($f.score)"
            Write-Host "    Reason: $($f.reason)"
        }
    }

    Write-Host "`n=========================================================="
    Write-Host "PHASE 6: LOGOUT & TOKEN REVOCATION"
    Write-Host "=========================================================="

    # Investigator logs out
    $logoutResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/logout" `
        -Method Post -Headers $invHeaders
    Write-Host "Investigator logged out successfully."

    # Verify old token is now revoked
    try {
        $revokedAttempt = Invoke-RestMethod -Uri "http://localhost:8080/api/cases" `
            -Method Get -Headers $invHeaders
        throw "Security failure: revoked token was accepted!"
    } catch {
        $revokedStatus = $_.Exception.Response.StatusCode.value__
        Write-Host "Revoked token correctly rejected with HTTP Status: $revokedStatus (Expected 401 Unauthorized)"
        if ($revokedStatus -ne 401) {
            throw "Expected HTTP 401 Unauthorized after logout, got $revokedStatus"
        }
    }

    Write-Host "`n=========================================================="
    Write-Host "ALL SECURITY & WORKFLOW VERIFICATION PHASES PASSED!"
    Write-Host "=========================================================="

} finally {
    Write-Host "`nCleaning up background microservices..."
    if ($m1Proc -and -not $m1Proc.HasExited) { Stop-Process -Id $m1Proc.Id -Force }
    if ($m2Proc -and -not $m2Proc.HasExited) { Stop-Process -Id $m2Proc.Id -Force }
    if ($m3Proc -and -not $m3Proc.HasExited) { Stop-Process -Id $m3Proc.Id -Force }
    Write-Host "All processes cleanly shut down."
}
