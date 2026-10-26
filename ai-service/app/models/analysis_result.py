from dataclasses import dataclass, field
from app.models.finding import Finding


@dataclass
class AnalysisResult:
    """Contains the complete output of M3 analysis for one investigation case."""

    total_events: int
    total_anomalies: int
    findings: list[Finding] = field(default_factory=list)
    statistics: dict = field(default_factory=dict)
    summary: str = ""
