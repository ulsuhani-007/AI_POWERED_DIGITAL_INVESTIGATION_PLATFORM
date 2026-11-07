-- =============================================================================
-- CyberTrace Platform - Migration V2: Forensic Chain of Custody & Integrity
-- Database: PostgreSQL
-- Author: Senior Software Engineer (AI-Powered Digital Investigation Platform)
-- =============================================================================

-- Add uploaded_by column to evidence table for chain-of-custody tracking
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS uploaded_by VARCHAR(64);

-- Ensure evidence uniqueness per case (case_id + hash_sha256)
CREATE UNIQUE INDEX IF NOT EXISTS uk_evidence_case_hash ON evidence(case_id, hash_sha256);

