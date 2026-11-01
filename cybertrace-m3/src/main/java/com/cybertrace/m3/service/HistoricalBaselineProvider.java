package com.cybertrace.m3.service;

import com.cybertrace.m3.model.NormalizedEventDTO;

import java.util.List;

/**
 * Abstraction for providing historical/legitimate baseline events used to train ML models.
 * Separates historical baseline evidence acquisition from live case analysis.
 */
public interface HistoricalBaselineProvider {

    /**
     * Retrieves the list of historical legitimate events used for model fitting.
     *
     * @return unmodifiable list of baseline events, or empty list if none available
     */
    List<NormalizedEventDTO> getBaselineEvents();
}

