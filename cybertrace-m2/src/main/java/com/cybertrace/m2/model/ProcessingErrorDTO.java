package com.cybertrace.m2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Audit record detailing parsing, validation, or structural errors encountered for a single row.
 */
public class ProcessingErrorDTO {

    @JsonProperty("line_number")
    private long lineNumber;

    @JsonProperty("raw_row_content")
    private String rawRowContent;

    @JsonProperty("reason")
    private String reason;

    public ProcessingErrorDTO() {
    }

    public ProcessingErrorDTO(long lineNumber, String rawRowContent, String reason) {
        this.lineNumber = lineNumber;
        this.rawRowContent = rawRowContent;
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getRawRowContent() {
        return rawRowContent;
    }

    public void setRawRowContent(String rawRowContent) {
        this.rawRowContent = rawRowContent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingErrorDTO that = (ProcessingErrorDTO) o;
        return lineNumber == that.lineNumber &&
                Objects.equals(rawRowContent, that.rawRowContent) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, rawRowContent, reason);
    }

    public static class Builder {
        private long lineNumber;
        private String rawRowContent;
        private String reason;

        public Builder lineNumber(long lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder rawRowContent(String rawRowContent) {
            this.rawRowContent = rawRowContent;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public ProcessingErrorDTO build() {
            return new ProcessingErrorDTO(lineNumber, rawRowContent, reason);
        }
    }
}
