package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateCaseRequest {

    @Size(max = 64, message = "Case number cannot exceed 64 characters")
    @JsonProperty("caseNumber")
    @JsonAlias({"case_number"})
    private String caseNumber;

    @Size(max = 255, message = "Case title cannot exceed 255 characters")
    @JsonProperty("title")
    @JsonAlias({"name"})
    private String title;

    @Size(max = 4000, message = "Case description cannot exceed 4000 characters")
    @JsonProperty("description")
    private String description;

    @Size(max = 32, message = "Status cannot exceed 32 characters")
    @JsonProperty("status")
    private String status;

    @Size(max = 32, message = "Priority cannot exceed 32 characters")
    @JsonProperty("priority")
    private String priority;

    @Size(max = 128, message = "Lead investigator cannot exceed 128 characters")
    @JsonProperty("leadInvestigator")
    @JsonAlias({"lead_investigator"})
    private String leadInvestigator;

    public UpdateCaseRequest() {}

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("name")
    public String getName() {
        return title;
    }

    @JsonProperty("name")
    public void setName(String name) {
        if (this.title == null || this.title.isBlank()) {
            this.title = name;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLeadInvestigator() {
        return leadInvestigator;
    }

    public void setLeadInvestigator(String leadInvestigator) {
        this.leadInvestigator = leadInvestigator;
    }
}

