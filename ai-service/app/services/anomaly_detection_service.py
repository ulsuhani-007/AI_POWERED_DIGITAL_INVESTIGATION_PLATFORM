from datetime import datetime
from typing import Any
from app.models.event import Event
from app.models.finding import Finding
from app.models.analysis_result import AnalysisResult


class AnomalyDetectionService:
    """Service responsible for analyzing normalized forensic events and producing investigation insights."""

    def analyze(self, events: list[Event]) -> AnalysisResult:
        """
        Analyze a list of normalized events and calculate baseline statistics.

        Safely handles empty event streams and produces an initial AnalysisResult
        with comprehensive frequency and temporal statistics.
        """
        if not events:
            return AnalysisResult(
                total_events=0,
                total_anomalies=0,
                findings=[],
                statistics={
                    "events_by_type": {},
                    "events_by_source": {},
                    "successful_events": 0,
                    "failed_events": 0,
                    "events_with_bytes": 0,
                    "min_timestamp": None,
                    "max_timestamp": None,
                },
                summary="",
            )

        total_events = len(events)
        events_by_type: dict[str, int] = {}
        events_by_source: dict[str, int] = {}
        successful_events = 0
        failed_events = 0
        events_with_bytes = 0
        min_ts: datetime = events[0].timestamp
        max_ts: datetime = events[0].timestamp

        for event in events:
            # Count events by event_type
            events_by_type[event.event_type] = events_by_type.get(event.event_type, 0) + 1

            # Count events by source
            events_by_source[event.source] = events_by_source.get(event.source, 0) + 1

            # Count success/failure where available
            if event.success is True:
                successful_events += 1
            elif event.success is False:
                failed_events += 1

            # Count events with bytes metric available
            if event.bytes is not None:
                events_with_bytes += 1

            # Track temporal range
            if event.timestamp < min_ts:
                min_ts = event.timestamp
            if event.timestamp > max_ts:
                max_ts = event.timestamp

        statistics: dict[str, Any] = {
            "events_by_type": events_by_type,
            "events_by_source": events_by_source,
            "successful_events": successful_events,
            "failed_events": failed_events,
            "events_with_bytes": events_with_bytes,
            "min_timestamp": min_ts.isoformat(),
            "max_timestamp": max_ts.isoformat(),
        }

        return AnalysisResult(
            total_events=total_events,
            total_anomalies=0,
            findings=[],
            statistics=statistics,
            summary="",
        )
