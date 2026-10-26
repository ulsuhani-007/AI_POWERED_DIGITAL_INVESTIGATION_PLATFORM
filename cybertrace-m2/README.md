# CyberTrace AI — Module 2 (M2): Evidence Processing & Timeline Engine

Module 2 (M2) is the ingestion, normalization, validation, and chronological timeline engine of **CyberTrace AI**. It processes raw heterogeneous forensic CSV evidence files, isolates invalid or corrupted records (zero silent failures), standardizes event records into a strict JSON schema contract, and merges multi-source evidence into an ascending chronological timeline for downstream ML anomaly detection (M3), persistence (M1), and UI rendering (M4).

---

## 1. Architectural Highlights & Technology Stack

- **Runtime & Language:** Java 21 LTS
- **Framework:** Spring Boot 3.3.4 (Spring Web, Jakarta Validation, Jackson JSR310)
- **CSV Ingestion:** Apache Commons CSV 1.11.0 (resilient header case-insensitivity, whitespace trimming, BOM handling)
- **Design Pattern:** Strategy Pattern for category-specific evidence parsing (`EvidenceParser`), Factory Pattern (`EvidenceParserFactory`), and Dynamic Header Signature Detector (`EvidenceFileTypeDetector`).
- **Test Framework:** JUnit 5, AssertJ, Spring MockMvc (41 unit and end-to-end integration tests).

---

## 2. Ingestion Evidence Categories

| Category | Input CSV Filename | Supported Event Type | Expected Header Signature | Normalized Description Format |
|---|---|---|---|---|
| **User Logins** | `login.csv` | `LOGIN` | `timestamp, user, ipAddress, device, status` | `"User {user} login attempt: {status}"` |
| **Browser Activity** | `browser_activity.csv` | `BROWSER_ACTIVITY` | `timestamp, user, url, device` | `"User {user} visited URL: {url}"` |
| **File Operations** | `file_access.csv` | `FILE_ACCESS` | `timestamp, user, fileName, action, device` | `"User {user} executed action {action} on file {fileName}"` |
| **Network Traffic** | `network_events.csv` | `NETWORK_EVENT` | `timestamp, sourceIp, destinationIp, port, bytes, device` | `"Network traffic from {sourceIp} to {destinationIp}:{port} ({bytes} bytes)"` |

---

## 3. Strict Schema Contract (M2 → M1 / M3 / M4)

All normalized events conform to the following JSON schema. Fields not applicable to a specific event category are explicitly serialized as `null` (`JsonInclude.Include.ALWAYS`):

```json
{
  "event_id": "urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "case_id": "case-123",
  "timestamp": "2026-10-26T10:30:00",
  "event_type": "LOGIN",
  "user": "user1",
  "device": "device1",
  "ip_address": "192.168.1.10",
  "destination_ip": null,
  "file_name": null,
  "action": null,
  "url": null,
  "port": null,
  "bytes": null,
  "source_evidence_id": "evidence-123",
  "description": "User user1 login attempt: SUCCESS"
}
```

### Schema Rules Enforced
1. **Naming:** Snake_case keys (`@JsonProperty("...")`).
2. **Timestamp:** ISO-8601 representation without timezone offset drift (`yyyy-MM-dd'T'HH:mm:ss`).
3. **Explicit Nulls:** Non-applicable fields are never omitted.
4. **Deterministic Sorting:** Events sorted strictly ascending by `timestamp`, with deterministic secondary sorting by `event_type`, then `event_id`.
5. **Event ID:** Generated as `urn:uuid:<UUID>`.

---

## 4. Processing, Validation & Error Auditing (Zero Silent Failures)

1. **Multi-Format Timestamp Parser:**
   - Supports:
     - `yyyy-MM-dd'T'HH:mm:ss`
     - `yyyy-MM-dd HH:mm:ss`
     - `dd/MM/yyyy HH:mm:ss`
     - `yyyy-MM-dd'T'HH:mm:ss.SSS`
     - ISO UTC offsets (e.g. `2026-10-26T10:30:00Z`)
2. **Failure Auditing (`ProcessingErrorDTO`):**
   - Corrupted or invalid rows do **NOT** abort the batch.
   - Individual failures are isolated with line number, raw row content, and failure reason:
     - Missing column: `"Missing required column: timestamp"`
     - Corrupted date: `"Unparseable date: 26-99-2026"`
     - Blank lines: `"Blank row"`
     - Invalid port / bytes: `"Invalid port number: abc"` / `"Bytes cannot be negative: -50"`
3. **Deduplication:**
   - Detects duplicate records within the same batch based on signature: `timestamp + user + device + type/action + detail`.
   - Skips duplicates while recording audit notices in `errors`.

---

## 5. REST API Endpoints

### 1. Ingest & Process Evidence
- **Endpoint:** `POST /api/v1/evidence/upload-and-process`
- **Content-Type:** `multipart/form-data`
- **Parameters:**
  - `caseId` (String, required): Case ID
  - `sourceEvidenceId` (String, required): Evidence artifact ID
  - `fileType` (Enum: `LOGIN`, `BROWSER`, `FILE_ACCESS`, `NETWORK`, `AUTO_DETECT`; default `AUTO_DETECT`)
  - `file` (MultipartFile, required): Raw CSV file
- **Response (`200 OK`):**
```json
{
  "case_id": "case-123",
  "source_evidence_id": "evidence-123",
  "total_processed_rows": 4,
  "valid_events_count": 4,
  "error_count": 0,
  "events": [ ... ],
  "errors": [ ... ]
}
```

### 2. Merge Multi-Source Timelines
- **Endpoint:** `POST /api/v1/evidence/timeline/merge`
- **Content-Type:** `application/json`
- **Payload:** `List<NormalizedEventDTO>`
- **Response (`200 OK`):**
```json
{
  "total_events": 16,
  "start_time": "2026-10-26T09:30:00",
  "end_time": "2026-10-26T12:00:00",
  "case_ids": ["case-123", "case-456"],
  "source_evidence_ids": ["ev-login-01", "ev-network-01"],
  "events": [ ... ]
}
```

---

## 6. How to Build & Run

### Prerequisites
- Java 21 or higher
- Maven (included via `./mvnw.cmd` wrapper)

### Running Tests
```bash
./mvnw.cmd test
```

### Running Application
```bash
./mvnw.cmd spring-boot:run
```
The server will start on port `8080`.
