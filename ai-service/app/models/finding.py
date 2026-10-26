from dataclasses import dataclass
from datetime import datetime


@dataclass
class Finding:
    """Represents suspicious or anomalous activity detected during M3 analysis."""

    event_id: str
    timestamp: datetime
    event_type: str
    reason: str
    severity: str
    score: float
    detection_method: str
