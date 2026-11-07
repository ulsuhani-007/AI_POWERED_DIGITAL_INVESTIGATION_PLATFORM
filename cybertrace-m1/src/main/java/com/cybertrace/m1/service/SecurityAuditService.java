package com.cybertrace.m1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Dedicated Security and Forensic Audit Service.
 * Logs critical security and forensic lifecycle events with strict data hygiene:
 * - Passwords, hashes, and raw bearer tokens are never logged.
 * - Structured audit messages allow automated ingestion by SIEM/monitoring tools.
 */
@Service
public class SecurityAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("com.cybertrace.audit");

    public void logLoginSuccess(String username, String clientIp) {
        auditLog.info("[AUDIT:LOGIN_SUCCESS] timestamp={} username='{}' clientIp='{}'",
                Instant.now(), sanitize(username), sanitize(clientIp));
    }

    public void logLoginFailure(String attemptedUsername, String clientIp, String reason) {
        auditLog.warn("[AUDIT:LOGIN_FAILURE] timestamp={} attemptedUsername='{}' clientIp='{}' reason='{}'",
                Instant.now(), sanitize(attemptedUsername), sanitize(clientIp), sanitize(reason));
    }

    public void logLogout(String username, String clientIp) {
        auditLog.info("[AUDIT:LOGOUT] timestamp={} username='{}' clientIp='{}'",
                Instant.now(), sanitize(username), sanitize(clientIp));
    }

    public void logUserCreated(String adminUsername, String createdUsername, String role) {
        auditLog.info("[AUDIT:USER_CREATED] timestamp={} adminUser='{}' createdUser='{}' role={}",
                Instant.now(), sanitize(adminUsername), sanitize(createdUsername), role);
    }

    public void logUserDeleted(String adminUsername, String targetUsername) {
        auditLog.warn("[AUDIT:USER_DELETED] timestamp={} adminUser='{}' targetUser='{}'",
                Instant.now(), sanitize(adminUsername), sanitize(targetUsername));
    }

    public void logEvidenceUploaded(String caseId, String evidenceId, String fileName, String hashSha256, String uploadedBy) {
        auditLog.info("[AUDIT:EVIDENCE_UPLOADED] timestamp={} caseId='{}' evidenceId='{}' fileName='{}' sha256='{}' uploadedBy='{}'",
                Instant.now(), caseId, evidenceId, sanitize(fileName), hashSha256, sanitize(uploadedBy));
    }

    public void logEvidenceDuplicateRejected(String caseId, String fileName, String hashSha256, String attemptedBy) {
        auditLog.warn("[AUDIT:EVIDENCE_DUPLICATE_REJECTED] timestamp={} caseId='{}' fileName='{}' sha256='{}' attemptedBy='{}' action=REJECTED_409",
                Instant.now(), caseId, sanitize(fileName), hashSha256, sanitize(attemptedBy));
    }

    public void logEvidenceProcessed(String evidenceId, int eventCount, String status) {
        auditLog.info("[AUDIT:EVIDENCE_PROCESSED] timestamp={} evidenceId='{}' eventCount={} status={}",
                Instant.now(), evidenceId, eventCount, status);
    }

    public void logAnalysisExecuted(String caseId, int totalEvents, int anomaliesDetected) {
        auditLog.info("[AUDIT:ANALYSIS_EXECUTED] timestamp={} caseId='{}' totalEvents={} anomaliesDetected={}",
                Instant.now(), caseId, totalEvents, anomaliesDetected);
    }

    private String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[\r\n]", "_");
    }
}

