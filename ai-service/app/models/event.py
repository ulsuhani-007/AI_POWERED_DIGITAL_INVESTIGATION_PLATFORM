from dataclasses import dataclass
from datetime import datetime
from typing import Optional


@dataclass
class Event:
    """Represents one processed digital-investigation event supplied to the M3 analysis pipeline."""

    id: str
    timestamp: datetime
    source: str
    event_type: str
    user: Optional[str] = None
    ip_address: Optional[str] = None
    device: Optional[str] = None
    success: Optional[bool] = None
    bytes: Optional[int] = None
    description: Optional[str] = None
