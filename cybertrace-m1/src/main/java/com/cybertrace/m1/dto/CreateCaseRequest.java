package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCaseRequest {

    @Size(max = 64, message = "Case number cannot exceed 64 characters")
    @JsonProperty("caseNumber")
    @JsonAlias({"case_number"})
    private String caseNumber;

    @NotBlank(message = "Case title is required")
    @Size(min = 1, max = 255, message = "Case title must be between 1 and 255 characters")
    @JsonProperty("title")
    @JsonAlias({"name"})
    private String title;

    @Size(max = 4000, message = "Case description cannot exceed 4000 characters")
    @JsonProperty("description")
    private String description;

    @Size(max = 32, message = "Priority cannot exceed 32 characters")
    @JsonProperty("priority")
    private String priority;

    @Size(max = 128, message = "Lead investigator cannot exceed 128 characters")
    @JsonProperty("leadInvestigator")
    @JsonAlias({"lead_investigator"})
    private String leadInvestigator;

    public CreateCaseRequest() {}

    public CreateCaseRequest(String caseNumber, String title, String description,
                             String priority, String leadInvestigator) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.leadInvestigator = leadInvestigator;
    }

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

