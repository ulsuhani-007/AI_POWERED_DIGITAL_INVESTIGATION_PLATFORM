-- =============================================================================
-- CyberTrace Platform - Initial Schema Migration (V1)
-- Database: PostgreSQL
-- Author: Senior Software Engineer (AI-Powered Digital Investigation Platform)
-- =============================================================================

-- 1. Investigation Cases
CREATE TABLE IF NOT EXISTS cases (
    id VARCHAR(64) PRIMARY KEY,
    case_number VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    lead_investigator VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cases_case_number ON cases(case_number);
CREATE INDEX IF NOT EXISTS idx_cases_status ON cases(status);

-- 2. Evidence Metadata & Integrity Hashes
CREATE TABLE IF NOT EXISTS evidence (
    id VARCHAR(64) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64),
    file_size_bytes BIGINT,
    hash_sha256 VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(128),
    description TEXT,
    event_count INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_evidence_case_id ON evidence(case_id);
CREATE INDEX IF NOT EXISTS idx_evidence_hash ON evidence(hash_sha256);

-- 3. Normalized Forensic Events Timeline
CREATE TABLE IF NOT EXISTS normalized_events (
    event_id VARCHAR(128) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    user_name VARCHAR(128),
    device VARCHAR(128),
    ip_address VARCHAR(64),
    destination_ip VARCHAR(64),
    file_name VARCHAR(255),
    action VARCHAR(128),
    url VARCHAR(1024),
    port INTEGER,
    bytes BIGINT,
    source_evidence_id VARCHAR(128),
    description TEXT,
    success BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_events_case_id ON normalized_events(case_id);
CREATE INDEX IF NOT EXISTS idx_events_timestamp ON normalized_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_events_type ON normalized_events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_source_evidence ON normalized_events(source_evidence_id);

-- 4. Forensic Analysis Reports & Findings
CREATE TABLE IF NOT EXISTS analysis_reports (
    id VARCHAR(64) PRIMARY KEY,
    case_id VARCHAR(64) NOT NULL UNIQUE REFERENCES cases(id) ON DELETE CASCADE,
    total_events INTEGER NOT NULL DEFAULT 0,
    total_anomalies INTEGER NOT NULL DEFAULT 0,
    findings_json TEXT,
    statistics_json TEXT,
    summary TEXT,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_analysis_case_id ON analysis_reports(case_id);

-- 5. Platform Users & Access Control
CREATE TABLE IF NOT EXISTS platform_users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username ON platform_users(username);

-- 6. Authentication Session Tokens
CREATE TABLE IF NOT EXISTS auth_tokens (
    token VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES platform_users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_token_expires ON auth_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_token_user_id ON auth_tokens(user_id);

