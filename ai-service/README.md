# AI & Anomaly Detection Service

Member 3 module for AI-powered digital investigation, anomaly detection, statistical analysis, and automated investigative summaries.

## Structure

- `app/models/` — Data models such as events and findings
- `app/services/` — Anomaly detection, statistics, and summary services
- `app/utils/` — Reusable helper functions
- `tests/` — M3 tests
- `requirements.txt` — Python dependencies

## M2 → M3 Data Contract

Member 2 (Evidence Processing) is responsible for ingesting, parsing, and normalizing raw evidence, and providing clean, normalized events to Member 3 (AI/ML Module).

### Canonical Event Fields (Initial Contract)

- `id`: string — unique event identifier
- `timestamp`: UTC datetime — normalized event timestamp
- `source`: string — original evidence source
- `event_type`: string — normalized event category
- `user`: optional string
- `ip_address`: optional string
- `device`: optional string
- `success`: optional boolean
- `bytes`: optional integer
- `description`: optional string

### Contract Rules

1. M2 is responsible for parsing raw evidence.
2. M2 is responsible for timestamp normalization.
3. M2 is responsible for cleaning and validating records.
4. M2 is responsible for assigning normalized event types.
5. M3 must receive normalized events, not raw CSV/log parsing output.
6. Every event must have a unique `id` so M3 findings can trace back to the original evidence.
7. Do not invent event types or fields that are not supported by actual evidence.
8. The contract may be extended later when real evidence is added.
