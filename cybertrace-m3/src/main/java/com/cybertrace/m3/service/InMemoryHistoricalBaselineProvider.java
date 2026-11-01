package com.cybertrace.m3.service;

import com.cybertrace.m3.model.NormalizedEventDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of {@link HistoricalBaselineProvider}.
 * Serves as the internal abstraction allowing baseline data to be supplied separately
 * from incoming case events, without altering the public M2 -> M3 API contract.
 */
@Component
public class InMemoryHistoricalBaselineProvider implements HistoricalBaselineProvider {

    private final List<NormalizedEventDTO> baselineEvents = new CopyOnWriteArrayList<>();

    public InMemoryHistoricalBaselineProvider() {
    }

    public InMemoryHistoricalBaselineProvider(List<NormalizedEventDTO> initialEvents) {
        if (initialEvents != null) {
            this.baselineEvents.addAll(initialEvents);
        }
    }

    @Override
    public List<NormalizedEventDTO> getBaselineEvents() {
        return Collections.unmodifiableList(new ArrayList<>(baselineEvents));
    }

    /**
     * Replaces the current baseline events with the provided collection.
     *
     * @param events legitimate baseline events
     */
    public void setBaselineEvents(List<NormalizedEventDTO> events) {
        this.baselineEvents.clear();
        if (events != null) {
            this.baselineEvents.addAll(events);
        }
    }

    /**
     * Appends additional baseline events to the existing baseline.
     *
     * @param events legitimate baseline events to append
     */
    public void addBaselineEvents(List<NormalizedEventDTO> events) {
        if (events != null) {
            this.baselineEvents.addAll(events);
        }
    }

    /**
     * Clears all baseline events.
     */
    public void clear() {
        this.baselineEvents.clear();
    }
}

