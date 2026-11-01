# CyberTrace AI — Module 3 (M3): AI & Anomaly Detection Foundation

Module 3 (M3) is the forensic analysis, anomaly detection, statistical profiling, and investigation summarization engine of **CyberTrace AI**. It consumes validated, normalized digital evidence events from Module 2 (M2), evaluates events across both rule-based forensic heuristics and multi-dimensional machine learning (Isolation Forest), correlates findings chronologically, computes baseline investigation metrics, and generates a grounded investigation summary for presentation in Module 4 (M4) and persistence in Module 1 (M1).

---

## 1. Architectural Highlights & Technology Stack

- **Runtime & Language:** Java 21 LTS
- **Framework:** Spring Boot 3.3.4 (Spring Web, Jakarta Validation, Jackson JSR310)
- **Machine Learning Library:** Haifeng Li's Smile Core 3.1.0 (`smile-core`)
- **Design Principles:**
  - Strict separation of training (historical baseline) from inference (case events).
  - High forensic traceability: every finding links back to specific evidence `event_id`s.
  - Zero hallucination: summaries are 100% grounded in analysis metrics and observed evidence.
  - Clean abstractions: `HistoricalBaselineProvider` SPI decouples baseline retrieval from detection.
  - Defensive engineering: null-safe handling, zero division protection, sub-minimum batch guards.
- **Test Framework:** JUnit 5, AssertJ, Spring MockMvc (127 unit and orchestration tests passing).

---

## 2. End-to-End Analysis Architecture

```
                    M2 NORMALIZED EVENTS (JSON)
                                │
                                ▼
                         AnalysisService
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
   Brute Force           Network Volume             Off-Hours
 (Sliding Window)     (Median + MAD Z-Score)   (Temporal Window)
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
                                ▼
                    ML Anomaly Detection
                                ▲
                                │  [Pre-fitted from HistoricalBaselineProvider]
                                │  (Smile 3.1.0 Isolation Forest)
                                │
                                ▼
                       Unified Findings
             (Deduplicated, Chronologically Sorted)
                                │
                                ▼
                    Investigation Statistics
                  (Counts by Type & Severity)
                                │
                                ▼
                 Grounded Investigation Summary
              (Deterministic Factual Synthesis)
                                │
                                ▼
                        AnalysisReportDTO
                                │
                                ▼
                 REST API: POST /api/v1/analysis/analyze
```

---

## 3. Forensic Anomaly Detectors

### 3.1 Brute Force Detection (`BruteForceDetectionService`)
- **Scope:** Targets `LOGIN` events with `status == FAILED`.
- **Grouping:** Grouped by composite key `(user, ip_address)` to distinguish distributed vs targeted attempts.
- **Window Algorithm:** Sliding time window (default: 5 minutes) identifying bursts meeting or exceeding the threshold (default: 5 attempts).
- **Graduated Severity:**
  - $\ge 3 \times \text{threshold}$ (15+ attempts): `CRITICAL`
  - $\ge 2 \times \text{threshold}$ (10+ attempts): `HIGH`
  - $\ge \text{threshold}$ (5+ attempts): `MEDIUM`
- **Traceability:** Links to the exact triggering event ID and details attempt count, user, IP, and window duration.

### 3.2 Network Volume Detection (`NetworkVolumeDetectionService`)
- **Scope:** Targets `NETWORK_EVENT` items with valid non-negative byte counts.
- **Statistical Model:** Uses robust, non-parametric Median and Median Absolute Deviation (MAD):
  $$\text{Modified } Z = \frac{x_i - \text{Median}}{1.4826 \times \text{MAD}}$$
- **Resilience:** If $>50\%$ of byte values are identical ($\text{MAD} = 0$), falls back to Mean Absolute Deviation with consistency multiplier $1.2533$. If all values are identical ($\text{scale} \le 0$), safely returns zero anomalies.
- **Threshold:** Configurable threshold (default: $Z \ge 3.5$).
- **Severity:** `MEDIUM` ($Z \ge 3.5$), `HIGH` ($Z \ge 7.0$), `CRITICAL` ($Z \ge 10.5$).
- **Traceability:** Details transfer volume, source IP, destination IP:port, median baseline, and modified Z-score.

### 3.3 Off-Hours Activity Detection (`OffHoursDetectionService`)
- **Scope:** Evaluates sensitive operations (`LOGIN` and `FILE_ACCESS`) against configured working hours (default: `09:00` to `18:00`). Boundaries are considered normal business hours.
- **Cross-Midnight Support:** Supports both standard daytime schedules (e.g. 09:00–18:00) and cross-midnight shifts (e.g. 22:00–06:00).
- **Circular Distance Calculation:** Computes the shortest circular temporal distance in minutes to the nearest working-hour boundary.
- **Graduated Severity:**
  - $\le 2\text{ hours}$ outside window: `LOW` (e.g. staying slightly late or arriving early)
  - $2\text{ to } 5\text{ hours}$ outside window: `MEDIUM` (e.g. late evening / early morning)
  - $> 5\text{ hours}$ outside window: `HIGH` (e.g. deep night / graveyard hours like 01:00–05:00)
  - *Note:* Off-hours alone never generates `CRITICAL` severity to prevent false alarm fatigue.

---

## 4. Machine Learning Architecture (Isolation Forest)

### 4.1 Baseline vs. Case-Data Separation
To guarantee forensic integrity, `MachineLearningAnomalyDetectionService` enforces strict separation between model fitting and inference:

1. **Model Fitting (`fit(baselineEvents)`):**
   - Fitted exclusively on legitimate historical baseline events.
   - Computes baseline column statistics (means, standard deviations) and active feature variance masks.
   - Produces an immutable `TrainedModel` snapshot (`IsolationForest`, `baselineMeans`, `baselineStdDevs`, `activeCols`).
2. **Model Scoring (`score(caseEvents)`):**
   - Incoming case investigation events are evaluated strictly against the pre-fitted baseline forest.
   - **Case events are NEVER used to train the model, and are NEVER appended to the baseline.**
3. **Historical Baseline SPI (`HistoricalBaselineProvider`):**
   - Clean interface allowing historical baseline events to be supplied independently from the public case-analysis endpoint.
   - Shipped with `InMemoryHistoricalBaselineProvider` for immediate operational stability without requiring database configuration.

### 4.2 Multi-Dimensional Feature Extraction
Both training and scoring use an identical 6-dimensional deterministic numerical feature vector extracted from normalized events:

| Index | Feature Dimension | Transformation & Range | Forensic Justification |
|---|---|---|---|
| **[0]** | Hour of Day | `0.0` – `23.0` (default 12.0 if null) | Captures temporal activity deviations |
| **[1]** | Event Type Encoded | Categorical mapping: LOGIN (1.0), FILE_ACCESS (2.0), NETWORK_EVENT (3.0), BROWSER_ACTIVITY (4.0), Other (5.0) | Models expected baseline operation mix |
| **[2]** | Log-Scaled Bytes | $\ln(1 + \text{bytes})$ | Compresses heavy-tailed byte distributions without numerical skew |
| **[3]** | Normalized Port | $\min(\text{port}, 65535) / 65535.0$ | Maps destination port space to $[0.0, 1.0]$ |
| **[4]** | Operation Outcome | `1.0` if failed/failure status, `0.0` otherwise | Extracted from standardized description patterns |
| **[5]** | Action Encoded | Categorical mapping: READ (1.0), WRITE/MODIFY (2.0), COPY (3.0), EXPORT (4.0), DELETE (5.0), EXECUTE (6.0), Other (7.0) | Flags rare or dangerous operations |

### 4.3 Small-Data & Defensive Safety
- Null input, empty lists, or sub-minimum baseline batches (`< 5` events) safely bypass ML scoring with debug logging and zero exceptions.
- Zero-variance baseline detection: if all baseline samples are identical across active columns, the service safely deactivates training rather than building degenerate trees.
- If no model is fitted, the ML detector returns empty findings and allows rule-based detectors and summarization to proceed normally.

---

## 5. Grounded Forensic Summarization (`InvestigationSummaryService`)

The investigation summary is generated deterministically from the final `AnalysisReportDTO`:
1. **Overall Analysis:** Reports total events analyzed and total anomalies identified.
2. **Finding Breakdown:** Alphabetical listing of anomaly types and severity distribution ordered by precedence (`CRITICAL` $\to$ `HIGH` $\to$ `MEDIUM` $\to$ `LOW`).
3. **Key Findings:** Prioritizes findings of the highest severity present, detailing event ID, timestamp, forensic reason, and anomaly score.
4. **Conclusion:** Factual forensic conclusion indicating whether analyst inspection is warranted or events conform to expected patterns.
5. **Anti-Hallucination Guarantee:** Never references ungrounded personas, motives, or unverified claims.

---

## 6. REST API Specification

### Endpoint
```http
POST /api/v1/analysis/analyze
Content-Type: application/json
Accept: application/json
```

### Request Payload (M2 Normalized Events Contract)
```json
[
  {
    "event_id": "urn:uuid:e8b24874-bf83-4a0b-93d3-f5449755490a",
    "case_id": "case-2026-001",
    "timestamp": "2026-10-26T02:15:00",
    "event_type": "NETWORK_EVENT",
    "user": "analyst",
    "device": "WORKSTATION-01",
    "ip_address": "192.168.1.50",
    "destination_ip": "185.220.101.5",
    "port": 4444,
    "bytes": 9999999999,
    "file_name": null,
    "action": "EXPORT",
    "url": null,
    "source_evidence_id": "ev-pcap-01",
    "description": "Network traffic from 192.168.1.50 to 185.220.101.5:4444 (9999999999 bytes)"
  }
]
```

### Response Payload (`AnalysisReportDTO`)
```json
{
  "total_events": 1,
  "total_anomalies": 2,
  "findings": [
    {
      "event_id": "urn:uuid:e8b24874-bf83-4a0b-93d3-f5449755490a",
      "timestamp": "2026-10-26T02:15:00",
      "event_type": "NETWORK_EVENT",
      "anomaly_type": "NETWORK_VOLUME",
      "severity": "CRITICAL",
      "score": 0.99,
      "detection_method": "STATISTICAL_MEDIAN_ABSOLUTE_DEVIATION",
      "reason": "Network transfer of 9999999999 bytes from IP '192.168.1.50' to '185.220.101.5:4444' is unusually high..."
    },
    {
      "event_id": "urn:uuid:e8b24874-bf83-4a0b-93d3-f5449755490a",
      "timestamp": "2026-10-26T02:15:00",
      "event_type": "NETWORK_EVENT",
      "anomaly_type": "MACHINE_LEARNING",
      "severity": "MEDIUM",
      "score": 0.638,
      "detection_method": "Smile 3.1.0 Isolation Forest (trees=100, seed=42)",
      "reason": "Machine Learning anomaly detected (score: 0.638): unusually high data transfer volume of 9999999999 bytes (log-scale outlier)"
    }
  ],
  "statistics": {
    "total_events": 1,
    "events_by_type": { "NETWORK_EVENT": 1 },
    "events_by_source": { "ev-pcap-01": 1 },
    "findings_by_anomaly_type": { "NETWORK_VOLUME": 1, "MACHINE_LEARNING": 1 },
    "findings_by_severity": { "CRITICAL": 1, "MEDIUM": 1 }
  },
  "summary": "Overall Analysis:\n1 events analyzed across the timeline; 2 anomalies identified.\n\nFinding Breakdown:\n- Anomaly Types: MACHINE_LEARNING (1), NETWORK_VOLUME (1)\n- Severity Distribution: CRITICAL (1), MEDIUM (1)\n\nKey Findings:\n- [CRITICAL] NETWORK_VOLUME on event 'urn:uuid:e8b24874-bf83-4a0b-93d3-f5449755490a' at 2026-10-26T02:15:00: Network transfer...\n\nInvestigation Conclusion:\nSuspicious activity identified requiring analyst review. Prioritized findings warrant forensic inspection."
}
```

---

## 7. Testing & Verification

Execute the complete M3 test suite using the Maven wrapper:

```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Test Suite Execution Summary
- **Total Tests:** 127
- **Passed:** 127
- **Failed:** 0
- **Skipped:** 0
- **Elapsed Time:** ~13.3 seconds

| Test Class | Tests | Focus Area |
|---|---|---|
| `AnalysisControllerTest` | 11 | REST contract, HTTP status codes (200, 400, 415), JSON serialization |
| `ModelContractTest` | 4 | DTO immutability, builder contracts, JSON annotations |
| `NormalizedEventDTOTest` | 6 | M2 $\to$ M3 field contract, equals/hashCode, null-safe accessors |
| `AnalysisServiceTest` | 19 | Pipeline orchestration, multi-detector correlation, timestamp sorting |
| `BruteForceDetectionServiceTest` | 13 | Sliding window, failure extraction, user/IP bursts, severity scaling |
| `InvestigationSummaryServiceTest` | 14 | Grounded summaries, singular/plural, severity ranking, anti-hallucination |
| `MachineLearningAnomalyDetectionServiceTest` | 17 | Baseline fitting vs case scoring separation, non-mutation, Smile IF |
| `NetworkVolumeDetectionServiceTest` | 13 | Median + MAD modified Z-score, scale estimation, fallback handling |
| `OffHoursDetectionServiceTest` | 15 | Working-hour boundaries, circular distance, LOGIN/FILE_ACCESS filtering |
| `EventStatusExtractorTest` | 15 | Status parsing from descriptions, case-insensitivity, failure matching |

---

## 8. Current Limitations & Future Roadmap

1. **Historical Baseline Persistence:**
   - *Current State:* Uses `InMemoryHistoricalBaselineProvider`. Baseline events are kept in-memory for the runtime lifecycle.
   - *Future Work:* Implement a JPA/PostgreSQL `DatabaseHistoricalBaselineProvider` connected to Module 1 (M1) to persist historical baseline profiles across system restarts.
2. **Model Serialization & Background Retraining:**
   - *Current State:* Smile Isolation Forest models are trained synchronously in memory when baseline data is supplied.
   - *Future Work:* Introduce offline model serialization (`.bin` or ONNX format) with scheduled background training jobs for large-scale enterprise data volumes.
3. **Dynamic Per-Case Baseline Selection:**
   - *Current State:* The active baseline applies across case analysis runs.
   - *Future Work:* Allow case-specific baseline profiles tagged by organization or asset tier.

