package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for:
 * - POST /api/cases
 */
public class CreateCaseRequest {

    @JsonProperty("caseNumber")
    private String caseNumber;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("leadInvestigator")
    private String leadInvestigator;

    public CreateCaseRequest() {}

    public CreateCaseRequest(String caseNumber, String title, String description, String priority, String leadInvestigator) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.leadInvestigator = leadInvestigator;
    }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getLeadInvestigator() { return leadInvestigator; }
    public void setLeadInvestigator(String leadInvestigator) { this.leadInvestigator = leadInvestigator; }
}
