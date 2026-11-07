# CyberTrace AI — Module 1 (M1): Core Backend & Database Engine

Module 1 (M1) is the central orchestration, persistence, case lifecycle, and forensic integrity foundation of **CyberTrace AI**. It acts as the backbone of the platform, managing case records, cataloging uploaded evidence with cryptographic SHA-256 chain-of-custody verification, persisting normalized forensic events conforming to the M2 → M3 contract, and storing grounded AI anomaly detection reports for display in Module 4 (M4 JavaFX Frontend).

---

## 1. Architectural Highlights & Technology Stack

- **Runtime & Language:** Java 21 LTS
- **Framework:** Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Jakarta Validation)
- **Database Engine:** H2 In-Memory Database (with H2 Web Console at `/h2-console`)
- **Persistence Layer:** Hibernate ORM 6.5.3, Spring Data JPA Repositories
- **Evidence Integrity:** Native Java `java.security.MessageDigest` SHA-256 checksum calculation
- **Serialization:** Jackson JSR-310 with dual-naming support (`camelCase` and `snake_case`) ensuring 100% contract compatibility across M2, M3, and M4.
- **Port:** `8080` (Standard backend port expected by JavaFX `frontend-javafx` REST client).
- **Test Framework:** JUnit 5, AssertJ, Spring MockMvc (24 automated tests passing).

---

## 2. Relational Entity Architecture

```
                    ┌─────────────────────────┐
                    │       CaseEntity        │
                    ├─────────────────────────┤
                    │ id (UUID, PK)           │
                    │ case_number (UK)        │
                    │ title / name            │
                    │ description             │
                    │ status (OPEN, CLOSED...)│
                    │ priority (LOW, HIGH...) │
                    │ lead_investigator       │
                    │ created_at, updated_at  │
                    └────────────┬────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │ 1                     │ 1                     │ 1
         │ *                     │ *                     │ 1
         ▼                       ▼                       ▼
┌──────────────────┐   ┌───────────────────────┐   ┌──────────────────────┐
│  EvidenceEntity  │   │ NormalizedEventEntity │   │ AnalysisReportEntity │
├──────────────────┤   ├───────────────────────┤   ├──────────────────────┤
│ id (UUID, PK)    │   │ event_id (PK)         │   │ id (UUID, PK)        │
│ case_id (FK)     │   │ case_id (FK)          │   │ case_id (FK, UK)     │
│ file_name        │   │ timestamp             │   │ total_events         │
│ file_type        │   │ event_type            │   │ total_anomalies      │
│ file_size_bytes  │   │ user_name             │   │ findings_json (CLOB) │
│ hash_sha256      │   │ device                │   │ statistics_json(CLOB)│
│ uploaded_at      │   │ ip_address            │   │ summary (CLOB)       │
│ status           │   │ destination_ip        │   │ generated_at         │
│ source           │   │ file_name, action     │   └──────────────────────┘
│ description      │   │ url, port, bytes      │
│ event_count      │   │ source_evidence_id    │
└──────────────────┘   │ description, success  │
                       └───────────────────────┘
```

---

## 3. Implemented REST API Endpoints

### 3.1 Case Management
- `GET    /api/cases` — List all investigation cases with real-time evidence, event, and anomaly counts.
- `POST   /api/cases` — Create a new investigation case. Auto-generates unique `CASE-<YYYY>-<SEQ>` if omitted.
- `GET    /api/cases/{id}` — Retrieve case details by ID.
- `PUT    /api/cases/{id}` — Update case title, description, priority, status, or assigned lead investigator.
- `DELETE /api/cases/{id}` — Delete case and cascade-delete all associated evidence, events, and reports.

### 3.2 Evidence Management & Integrity
- `GET    /api/cases/{caseId}/evidence` — List all uploaded evidence records for a case.
- `POST   /api/cases/{caseId}/evidence` — Multipart form-data file upload (`file`, optional `description`). Computes SHA-256 hash automatically upon ingestion.

### 3.3 Normalized Event Persistence
- `GET    /api/cases/{caseId}/events` — Chronologically sorted event timeline for a case.
- `POST   /api/cases/{caseId}/events` — Batch ingestion of normalized events conforming to M2/M3 schema.

### 3.4 AI Analysis Reports
- `GET    /api/cases/{caseId}/analysis` — Retrieve AI analysis report for a case. Returns initial empty report if not yet analyzed.
- `POST   /api/cases/{caseId}/analysis` — Persist AI anomaly detection report with findings and statistical profile.

---

## 4. Execution & Verification

### Running Tests
```bash
./mvnw clean test
```

### Running the Application
```bash
./mvnw spring-boot:run
```
The application will start on `http://localhost:8080`.
H2 Console is accessible at `http://localhost:8080/h2-console` (`jdbc:h2:mem:cybertrace_db`).

