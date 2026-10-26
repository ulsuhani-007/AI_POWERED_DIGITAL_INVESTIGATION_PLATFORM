package com.cybertrace.m2.util;

import com.cybertrace.m2.model.NormalizedEventDTO;

import java.util.Comparator;

/**
 * Deterministic chronological comparator for normalized events.
 * Primary: timestamp ascending
 * Secondary: event_type ascending
 * Tertiary: event_id ascending
 */
public final class NormalizedEventComparator implements Comparator<NormalizedEventDTO> {

    public static final NormalizedEventComparator INSTANCE = new NormalizedEventComparator();

    private static final Comparator<NormalizedEventDTO> COMPARATOR = Comparator
            .comparing(NormalizedEventDTO::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(e -> e.getEventType() != null ? e.getEventType().name() : "", Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(NormalizedEventDTO::getEventId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Override
    public int compare(NormalizedEventDTO o1, NormalizedEventDTO o2) {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) return 1;
        if (o2 == null) return -1;
        return COMPARATOR.compare(o1, o2);
    }
}
