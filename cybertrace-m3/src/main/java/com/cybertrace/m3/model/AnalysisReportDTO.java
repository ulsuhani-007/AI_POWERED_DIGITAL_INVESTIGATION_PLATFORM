package com.cybertrace.m3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates the complete M3 analysis report for an investigation case.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AnalysisReportDTO {

    @JsonProperty("total_events")
    private int totalEvents;

    @JsonProperty("total_anomalies")
    private int totalAnomalies;

    @JsonProperty("findings")
    private List<AnomalyFindingDTO> findings = new ArrayList<>();

    @JsonProperty("statistics")
    private Map<String, Object> statistics = new LinkedHashMap<>();

    @JsonProperty("summary")
    private String summary;

    public AnalysisReportDTO() {
    }

    public AnalysisReportDTO(
            int totalEvents,
            int totalAnomalies,
            List<AnomalyFindingDTO> findings,
            Map<String, Object> statistics,
            String summary
    ) {
        this.totalEvents = totalEvents;
        this.totalAnomalies = totalAnomalies;
        this.findings = findings != null ? findings : new ArrayList<>();
        this.statistics = statistics != null ? statistics : new LinkedHashMap<>();
        this.summary = summary;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getTotalAnomalies() {
        return totalAnomalies;
    }

    public void setTotalAnomalies(int totalAnomalies) {
        this.totalAnomalies = totalAnomalies;
    }

    public List<AnomalyFindingDTO> getFindings() {
        return findings;
    }

    public void setFindings(List<AnomalyFindingDTO> findings) {
        this.findings = findings != null ? findings : new ArrayList<>();
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics != null ? statistics : new LinkedHashMap<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisReportDTO that = (AnalysisReportDTO) o;
        return totalEvents == that.totalEvents &&
                totalAnomalies == that.totalAnomalies &&
                Objects.equals(findings, that.findings) &&
                Objects.equals(statistics, that.statistics) &&
                Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalEvents, totalAnomalies, findings, statistics, summary);
    }

    @Override
    public String toString() {
        return "AnalysisReportDTO{" +
                "totalEvents=" + totalEvents +
                ", totalAnomalies=" + totalAnomalies +
                ", findings=" + findings +
                ", statistics=" + statistics +
                ", summary='" + summary + '\'' +
                '}';
    }

    public static class Builder {
        private int totalEvents;
        private int totalAnomalies;
        private List<AnomalyFindingDTO> findings = new ArrayList<>();
        private Map<String, Object> statistics = new LinkedHashMap<>();
        private String summary;

        public Builder totalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder totalAnomalies(int totalAnomalies) {
            this.totalAnomalies = totalAnomalies;
            return this;
        }

        public Builder findings(List<AnomalyFindingDTO> findings) {
            this.findings = findings != null ? findings : new ArrayList<>();
            return this;
        }

        public Builder statistics(Map<String, Object> statistics) {
            this.statistics = statistics != null ? statistics : new LinkedHashMap<>();
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public AnalysisReportDTO build() {
            return new AnalysisReportDTO(totalEvents, totalAnomalies, findings, statistics, summary);
        }
    }
}
