package com.investigation.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary metrics and recent activity for the Dashboard view.
 */
public class DashboardStats {
    private int totalCases;
    private int totalEvidenceRecords;
    private int totalProcessedEvents;
    private int totalAnomalies;
    private int openCasesCount;
    private int criticalAnomaliesCount;
    private List<ActivityItem> recentActivity = new ArrayList<>();

    public static class ActivityItem {
        private String id;
        private String title;
        private String description;
        private String type; // e.g., "CASE_CREATED", "EVIDENCE_UPLOADED", "ANOMALY_DETECTED"
        private Instant timestamp;
        private String caseNumber;

        public ActivityItem(String id, String title, String description, String type, Instant timestamp, String caseNumber) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.timestamp = timestamp;
            this.caseNumber = caseNumber;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public Instant getTimestamp() { return timestamp; }
        public String getCaseNumber() { return caseNumber; }
    }

    public DashboardStats() {}

    public DashboardStats(int totalCases, int totalEvidenceRecords, int totalProcessedEvents,
                          int totalAnomalies, int openCasesCount, int criticalAnomaliesCount,
                          List<ActivityItem> recentActivity) {
        this.totalCases = totalCases;
        this.totalEvidenceRecords = totalEvidenceRecords;
        this.totalProcessedEvents = totalProcessedEvents;
        this.totalAnomalies = totalAnomalies;
        this.openCasesCount = openCasesCount;
        this.criticalAnomaliesCount = criticalAnomaliesCount;
        this.recentActivity = recentActivity != null ? recentActivity : new ArrayList<>();
    }

    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }

    public int getTotalEvidenceRecords() { return totalEvidenceRecords; }
    public void setTotalEvidenceRecords(int totalEvidenceRecords) { this.totalEvidenceRecords = totalEvidenceRecords; }

    public int getTotalProcessedEvents() { return totalProcessedEvents; }
    public void setTotalProcessedEvents(int totalProcessedEvents) { this.totalProcessedEvents = totalProcessedEvents; }

    public int getTotalAnomalies() { return totalAnomalies; }
    public void setTotalAnomalies(int totalAnomalies) { this.totalAnomalies = totalAnomalies; }

    public int getOpenCasesCount() { return openCasesCount; }
    public void setOpenCasesCount(int openCasesCount) { this.openCasesCount = openCasesCount; }

    public int getCriticalAnomaliesCount() { return criticalAnomaliesCount; }
    public void setCriticalAnomaliesCount(int criticalAnomaliesCount) { this.criticalAnomaliesCount = criticalAnomaliesCount; }

    public List<ActivityItem> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<ActivityItem> recentActivity) { this.recentActivity = recentActivity; }
}
