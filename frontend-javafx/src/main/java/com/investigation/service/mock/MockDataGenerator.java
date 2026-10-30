package com.investigation.service.mock;

import com.investigation.model.AnalysisResultModel;
import com.investigation.model.CaseModel;
import com.investigation.model.DashboardStats;
import com.investigation.model.EventModel;
import com.investigation.model.EvidenceModel;
import com.investigation.model.FindingModel;
import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;
import com.investigation.model.enums.EvidenceStatus;
import com.investigation.model.enums.Severity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Fictional mock dataset generator for offline demonstration.
 * All generated records are clearly flagged with [SIMULATED MOCK DATA].
 */
public class MockDataGenerator {

    private final Map<String, CaseModel> cases = new LinkedHashMap<>();
    private final Map<String, List<EvidenceModel>> evidenceByCase = new LinkedHashMap<>();
    private final Map<String, List<EventModel>> eventsByCase = new LinkedHashMap<>();
    private final Map<String, AnalysisResultModel> analysisByCase = new LinkedHashMap<>();

    public MockDataGenerator() {
        initMockData();
    }

    private void initMockData() {
        Instant now = Instant.now();

        // 1. Cases
        CaseModel case1 = new CaseModel(
                "case-891",
                "CASE-2026-0891",
                "[MOCK] Project Apex Internal Data Exfiltration",
                "Investigation into suspected multi-gigabyte off-hours egress to foreign IP space from R&D workstation pool.",
                CaseStatus.IN_PROGRESS,
                CasePriority.CRITICAL,
                "Special Agent V. Kovacs",
                now.minus(4, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.HOURS),
                4,
                1420,
                8
        );

        CaseModel case2 = new CaseModel(
                "case-904",
                "CASE-2026-0904",
                "[MOCK] Distributed Credential Stuffing & Privilege Escalation",
                "Automated high-velocity authentication failures followed by domain controller ticket grant from unassigned subnet.",
                CaseStatus.OPEN,
                CasePriority.HIGH,
                "Cyber Analyst S. Chen",
                now.minus(2, ChronoUnit.DAYS),
                now.minus(18, ChronoUnit.MINUTES),
                3,
                940,
                5
        );

        CaseModel case3 = new CaseModel(
                "case-912",
                "CASE-2026-0912",
                "[MOCK] Ransomware Pre-Attack Reconnaissance",
                "Internal port scanning and SMB beaconing detected originating from engineering workstation DESKTOP-ENG-08.",
                CaseStatus.OPEN,
                CasePriority.HIGH,
                "Investigator M. Rossi",
                now.minus(1, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.HOURS),
                2,
                680,
                4
        );

        CaseModel case4 = new CaseModel(
                "case-870",
                "CASE-2026-0870",
                "[MOCK] Unauthorized USB Mass Storage in Air-Gapped Vault",
                "Peripheral bus event logged on air-gapped terminal VAULT-TERM-02 during weekend maintenance cycle.",
                CaseStatus.CLOSED,
                CasePriority.MEDIUM,
                "Lead Examiner D. Vance",
                now.minus(14, ChronoUnit.DAYS),
                now.minus(6, ChronoUnit.DAYS),
                3,
                320,
                2
        );

        cases.put(case1.getId(), case1);
        cases.put(case2.getId(), case2);
        cases.put(case3.getId(), case3);
        cases.put(case4.getId(), case4);

        // 2. Evidence for Case 1 (Project Apex)
        List<EvidenceModel> ev1 = new ArrayList<>(List.of(
                new EvidenceModel("ev-101", case1.getId(), "perimeter_firewall_egress.csv", "CSV / Network Logs",
                        4852910L, now.minus(3, ChronoUnit.DAYS), EvidenceStatus.ANALYZED, "Palo Alto NGFW - Gateway-01",
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", 620,
                        "Firewall flow records capturing outbound traffic across port 443 & 8443."),
                new EvidenceModel("ev-102", case1.getId(), "auth_syslog_rd_pool.csv", "CSV / Authentication",
                        1298400L, now.minus(3, ChronoUnit.DAYS), EvidenceStatus.ANALYZED, "Central Syslog SIEM",
                        "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4", 450,
                        "Kerberos and SSH access records for R&D workstation cluster."),
                new EvidenceModel("ev-103", case1.getId(), "endpoint_edr_rd_fin4.csv", "CSV / Host Telemetry",
                        3104500L, now.minus(2, ChronoUnit.DAYS), EvidenceStatus.ANALYZED, "CrowdStrike Falcon Sensor",
                        "a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0", 350,
                        "Process execution tree, DLL loads, and outbound socket connections on DESKTOP-RD-04."),
                new EvidenceModel("ev-104", case1.getId(), "pcap_sample_stream_8443.csv", "CSV / Packet Capture",
                        8492000L, now.minus(1, ChronoUnit.DAYS), EvidenceStatus.PROCESSING, "Zeek IDS Sensor",
                        "b4c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3", 0,
                        "Decrypted payload telemetry and flow statistics for destination IP 198.51.100.42.")
        ));
        evidenceByCase.put(case1.getId(), ev1);

        // Evidence for Case 2
        List<EvidenceModel> ev2 = new ArrayList<>(List.of(
                new EvidenceModel("ev-201", case2.getId(), "ad_security_event_log.csv", "CSV / Security Event Log",
                        6400120L, now.minus(2, ChronoUnit.DAYS), EvidenceStatus.ANALYZED, "Windows Domain Controller DC01",
                        "9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e0d9c8b", 580,
                        "Event ID 4625 (Failed Logon) and Event ID 4672 (Admin Logon) records."),
                new EvidenceModel("ev-202", case2.getId(), "vpn_gateway_access.csv", "CSV / VPN Session Logs",
                        2109400L, now.minus(1, ChronoUnit.DAYS), EvidenceStatus.ANALYZED, "Cisco AnyConnect Concentrator",
                        "3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e", 360,
                        "Multi-geo concurrent VPN connections for employee account 'jdoe'.")
        ));
        evidenceByCase.put(case2.getId(), ev2);

        // 3. Events for Case 1
        List<EventModel> events1 = new ArrayList<>(List.of(
                new EventModel("evt-1001", case1.getId(), "ev-102", now.minus(72, ChronoUnit.HOURS),
                        "auth_syslog", "AUTHENTICATION", "jdoe", "192.168.10.45", "DESKTOP-RD-04",
                        true, 420L, "User 'jdoe' logged into workstation DESKTOP-RD-04 via Kerberos SSO."),
                new EventModel("evt-1002", case1.getId(), "ev-103", now.minus(71, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES),
                        "endpoint_edr", "PROCESS_SPAWN", "jdoe", "192.168.10.45", "DESKTOP-RD-04",
                        true, 0L, "Process 'powershell.exe -enc <base64>' spawned by explorer.exe."),
                new EventModel("evt-1003", case1.getId(), "ev-103", now.minus(70, ChronoUnit.HOURS),
                        "endpoint_edr", "FILE_ACCESS", "jdoe", "192.168.10.45", "DESKTOP-RD-04",
                        true, 185000000L, "Bulk read access to encrypted repository '/srv/rd_shared/project_apex_schematics.tar.gz'."),
                new EventModel("evt-1004", case1.getId(), "ev-102", now.minus(68, ChronoUnit.HOURS),
                        "auth_syslog", "PRIVILEGE_ESCALATION", "svc_backup", "192.168.10.45", "DESKTOP-RD-04",
                        true, 0L, "Local account 'svc_backup' elevated to SYSTEM privilege via token impersonation."),
                new EventModel("evt-1005", case1.getId(), "ev-101", now.minus(67, ChronoUnit.HOURS),
                        "perimeter_firewall", "NETWORK_CONNECTION", "svc_backup", "198.51.100.42", "DESKTOP-RD-04",
                        true, 12000L, "Outbound TLS 1.3 handshake initiated to 198.51.100.42:8443 (known Tor Exit relay)."),
                new EventModel("evt-1006", case1.getId(), "ev-101", now.minus(66, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
                        "perimeter_firewall", "DATA_EXFILTRATION", "svc_backup", "198.51.100.42", "DESKTOP-RD-04",
                        true, 4820000000L, "Unusually massive encrypted stream of 4.82 GB transmitted to 198.51.100.42."),
                new EventModel("evt-1007", case1.getId(), "ev-103", now.minus(65, ChronoUnit.HOURS),
                        "endpoint_edr", "LOG_CLEARING", "SYSTEM", "192.168.10.45", "DESKTOP-RD-04",
                        true, 0L, "Command 'wevtutil cl Security' executed to purge local audit log history."),
                new EventModel("evt-1008", case1.getId(), "ev-102", now.minus(48, ChronoUnit.HOURS),
                        "auth_syslog", "FAILED_AUTH", "jdoe", "203.0.113.88", "GATEWAY-VPN",
                        false, 0L, "Failed VPN authorization for 'jdoe' from Bucharest, Romania while user active locally in Zurich.")
        ));
        eventsByCase.put(case1.getId(), events1);

        // Events for Case 2
        List<EventModel> events2 = new ArrayList<>(List.of(
                new EventModel("evt-2001", case2.getId(), "ev-201", now.minus(30, ChronoUnit.HOURS),
                        "ad_security", "FAILED_AUTH", "root", "192.168.20.100", "DC01",
                        false, 0L, "Failed login attempt for nonexistent user 'root'."),
                new EventModel("evt-2002", case2.getId(), "ev-201", now.minus(30, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES),
                        "ad_security", "FAILED_AUTH", "administrator", "192.168.20.100", "DC01",
                        false, 0L, "Failed Kerberos pre-auth attempt for 'administrator'."),
                new EventModel("evt-2003", case2.getId(), "ev-201", now.minus(29, ChronoUnit.HOURS),
                        "ad_security", "PASSWORD_SPRAY", "svc_mssql", "192.168.20.100", "DC01",
                        true, 0L, "Successful login for service account 'svc_mssql' after 34 previous failure events.")
        ));
        eventsByCase.put(case2.getId(), events2);

        // 4. Findings & Analysis for Case 1
        List<FindingModel> findings1 = new ArrayList<>(List.of(
                new FindingModel("fnd-101", "evt-1006", now.minus(66, ChronoUnit.HOURS),
                        "DATA_EXFILTRATION",
                        "[MOCK] High-Volume Egress Burst (4.82 GB) during unapproved midnight window.",
                        Severity.CRITICAL, 0.98, "Statistical Z-Score (z=5.4) & Egress Volume Heuristic",
                        "198.51.100.42 / svc_backup", "Isolate workstation DESKTOP-RD-04 and block destination IP at border firewall."),
                new FindingModel("fnd-102", "evt-1005", now.minus(67, ChronoUnit.HOURS),
                        "SUSPICIOUS_IP",
                        "[MOCK] Connection established to high-confidence Tor Exit Node / Bulletproof Host.",
                        Severity.CRITICAL, 0.94, "Threat Intelligence Match (TOR_EXIT_LIST_V4)",
                        "198.51.100.42", "Review edge proxy logs for other internal endpoints contacting this destination."),
                new FindingModel("fnd-103", "evt-1004", now.minus(68, ChronoUnit.HOURS),
                        "PRIVILEGE_ESCALATION",
                        "[MOCK] Local service token escalation to SYSTEM without interactive session.",
                        Severity.HIGH, 0.89, "Behavioral Anomaly Engine (Isolation Forest)",
                        "svc_backup on DESKTOP-RD-04", "Revoke cached credentials for svc_backup and inspect service binPath."),
                new FindingModel("fnd-104", "evt-1007", now.minus(65, ChronoUnit.HOURS),
                        "ANTI_FORENSICS",
                        "[MOCK] Security event log truncation detected via wevtutil command.",
                        Severity.HIGH, 0.87, "Rule-Based Anti-Forensics Pattern Matcher",
                        "DESKTOP-RD-04", "Acquire live volatile memory image before powering off host."),
                new FindingModel("fnd-105", "evt-1008", now.minus(48, ChronoUnit.HOURS),
                        "IMPOSSIBLE_TRAVEL",
                        "[MOCK] Geographic velocity violation: Zurich local session vs Bucharest VPN attempt (2,100 km/h).",
                        Severity.HIGH, 0.85, "Geo-Velocity Correlator",
                        "jdoe", "Enforce emergency MFA reset on account 'jdoe'."),
                new FindingModel("fnd-106", "evt-1002", now.minus(71, ChronoUnit.HOURS),
                        "SUSPICIOUS_EXECUTION",
                        "[MOCK] Encoded PowerShell execution invoking unquoted process launch.",
                        Severity.MEDIUM, 0.72, "Process Lineage Tree Classifier",
                        "powershell.exe", "Decode and extract Base64 payload script from EDR telemetry."),
                new FindingModel("fnd-107", "evt-1003", now.minus(70, ChronoUnit.HOURS),
                        "ABNORMAL_FILE_ACCESS",
                        "[MOCK] Access to restricted directory outside regular user project scope.",
                        Severity.MEDIUM, 0.68, "File Access Baseline Deviance",
                        "/srv/rd_shared/project_apex_schematics.tar.gz", "Cross-reference access timestamp with project clearance register.")
        ));

        Map<String, Object> stats1 = new LinkedHashMap<>();
        stats1.put("total_events_scanned", 1420);
        stats1.put("anomalies_detected", 7);
        stats1.put("max_anomaly_score", 0.98);
        stats1.put("avg_anomaly_score", 0.85);
        stats1.put("primary_threat_vector", "Off-Hours Bulk Data Exfiltration");
        stats1.put("suspect_entities", List.of("198.51.100.42", "svc_backup", "DESKTOP-RD-04", "jdoe"));

        String summary1 = """
                ================================================================================
                [SIMULATED AI INVESTIGATIVE SUMMARY - CASE-2026-0891]
                ================================================================================
                INVESTIGATION OVERVIEW:
                Automated multi-source correlation indicates a high-confidence digital espionage incident
                targeting proprietary R&D blueprints ('Project Apex'). Activity commenced with legitimate
                credentials for user 'jdoe' on workstation DESKTOP-RD-04, followed by rapid internal
                privilege escalation using the local 'svc_backup' service token.

                KEY FINDINGS:
                1. Data Exfiltration: 4.82 GB of compressed schematics were transferred over TLS 1.3
                   to destination IP 198.51.100.42 (correlated with active Tor Exit infrastructure)
                   at 02:30:15 UTC.
                2. Anti-Forensics: The adversary executed 'wevtutil cl Security' shortly after transmission,
                   attempting to erase local Windows event logs.
                3. Credential Compromise: An impossible travel anomaly was flagged 18 hours later when
                   the compromised account attempted concurrent VPN authentication from Bucharest.

                INVESTIGATIVE LEADS & RECOMMENDATIONS:
                - Immediately quarantine host DESKTOP-RD-04 (IP: 192.168.10.45) from corporate VLAN.
                - Blacklist external IP 198.51.100.42 and domain apex-cdn-proxy.net at perimeter firewalls.
                - Preserve disk and volatile RAM snapshot for forensic timeline reconstruction.
                - Revoke and rotate service account 'svc_backup' master credentials across Active Directory.

                *DISCLAIMER: AI findings represent automated investigative hypotheses and leads.
                They do not constitute definitive legal proof of guilt and require manual examiner verification.*
                """;

        AnalysisResultModel analysis1 = new AnalysisResultModel(
                case1.getId(), 1420, findings1.size(), findings1, stats1, summary1, now.minus(1, ChronoUnit.HOURS)
        );
        analysisByCase.put(case1.getId(), analysis1);
    }

    public List<CaseModel> getAllCases() {
        return new ArrayList<>(cases.values());
    }

    public CaseModel getCaseById(String id) {
        return cases.get(id);
    }

    public CaseModel createCase(CaseModel caseModel) {
        if (caseModel.getId() == null || caseModel.getId().isBlank()) {
            caseModel.setId("case-" + (System.currentTimeMillis() % 10000));
        }
        if (caseModel.getCaseNumber() == null || caseModel.getCaseNumber().isBlank()) {
            caseModel.setCaseNumber("CASE-" + (2026) + "-" + String.format("%04d", new Random().nextInt(9000) + 1000));
        }
        caseModel.setCreatedAt(Instant.now());
        caseModel.setUpdatedAt(Instant.now());
        cases.put(caseModel.getId(), caseModel);
        return caseModel;
    }

    public CaseModel updateCase(CaseModel caseModel) {
        if (caseModel.getId() != null && cases.containsKey(caseModel.getId())) {
            caseModel.setUpdatedAt(Instant.now());
            cases.put(caseModel.getId(), caseModel);
            return caseModel;
        }
        return caseModel;
    }

    public boolean deleteCase(String id) {
        return cases.remove(id) != null;
    }

    public List<EvidenceModel> getEvidenceByCase(String caseId) {
        return evidenceByCase.getOrDefault(caseId, Collections.emptyList());
    }

    public void addEvidence(String caseId, EvidenceModel evidence) {
        evidenceByCase.computeIfAbsent(caseId, k -> new ArrayList<>()).add(evidence);
        CaseModel cm = cases.get(caseId);
        if (cm != null) {
            cm.setEvidenceCount(evidenceByCase.get(caseId).size());
        }
    }

    public List<EventModel> getEventsByCase(String caseId) {
        return eventsByCase.getOrDefault(caseId, Collections.emptyList());
    }

    public AnalysisResultModel getAnalysisByCase(String caseId) {
        if (analysisByCase.containsKey(caseId)) {
            return analysisByCase.get(caseId);
        }
        // Generate on-demand mock analysis if not precomputed
        CaseModel cm = cases.get(caseId);
        int totalEv = cm != null ? cm.getEventCount() : 120;
        int totalAnom = cm != null ? cm.getAnomalyCount() : 3;

        List<FindingModel> findings = new ArrayList<>();
        findings.add(new FindingModel("fnd-gen-1", "evt-gen-1", Instant.now().minus(2, ChronoUnit.HOURS),
                "SUSPICIOUS_LOGIN", "[MOCK] Uncharacteristic off-hours administrative login pattern.",
                Severity.HIGH, 0.88, "Isolation Forest Model", "admin_user", "Review admin active directory sessions."));

        Map<String, Object> stats = Map.of(
                "total_events_scanned", totalEv,
                "anomalies_detected", totalAnom,
                "max_anomaly_score", 0.88,
                "confidence_rating", "HIGH [SIMULATED]"
        );

        String summary = "[MOCK] Case " + (cm != null ? cm.getCaseNumber() : caseId) +
                " automated scan completed. Detected " + totalAnom + " anomalies across " + totalEv + " normalized events.";

        AnalysisResultModel generated = new AnalysisResultModel(
                caseId, totalEv, totalAnom, findings, stats, summary, Instant.now()
        );
        analysisByCase.put(caseId, generated);
        return generated;
    }

    public DashboardStats getDashboardStats() {
        int totalCases = cases.size();
        int totalEvidence = evidenceByCase.values().stream().mapToInt(List::size).sum();
        int totalEvents = 14820; // Aggregated realistic figure
        int totalAnomalies = 38;
        int openCases = (int) cases.values().stream()
                .filter(c -> c.getStatus() == CaseStatus.OPEN || c.getStatus() == CaseStatus.IN_PROGRESS).count();
        int criticalAnomalies = 7;

        List<DashboardStats.ActivityItem> activity = List.of(
                new DashboardStats.ActivityItem("act-1", "Massive Data Egress Flagged",
                        "4.82 GB outbound TLS transfer flagged by Z-Score Anomaly engine.", "ANOMALY",
                        Instant.now().minus(15, ChronoUnit.MINUTES), "CASE-2026-0891"),
                new DashboardStats.ActivityItem("act-2", "New Evidence Ingested",
                        "File 'perimeter_firewall_egress.csv' normalized with 620 events.", "EVIDENCE",
                        Instant.now().minus(45, ChronoUnit.MINUTES), "CASE-2026-0891"),
                new DashboardStats.ActivityItem("act-3", "Case Status Updated",
                        "Case 'CASE-2026-0904' assigned to Cyber Analyst S. Chen.", "CASE_UPDATE",
                        Instant.now().minus(2, ChronoUnit.HOURS), "CASE-2026-0904"),
                new DashboardStats.ActivityItem("act-4", "Impossible Travel Detected",
                        "Simultaneous auth attempt from Zurich & Bucharest within 2 minutes.", "ANOMALY",
                        Instant.now().minus(3, ChronoUnit.HOURS), "CASE-2026-0891"),
                new DashboardStats.ActivityItem("act-5", "Case Created",
                        "Investigation 'CASE-2026-0912: Ransomware Reconnaissance' opened.", "CASE_CREATED",
                        Instant.now().minus(5, ChronoUnit.HOURS), "CASE-2026-0912")
        );

        return new DashboardStats(totalCases, totalEvidence, totalEvents, totalAnomalies, openCases, criticalAnomalies, activity);
    }
}
