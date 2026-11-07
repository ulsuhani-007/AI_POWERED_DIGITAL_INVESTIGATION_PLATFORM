package com.investigation.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

/**
 * Custom Jackson deserializer that flexibly parses Instant from:
 * - ISO-8601 with timezone (e.g. 2026-10-26T10:30:15Z)
 * - ISO-8601 local date-time without timezone (e.g. 2026-10-26T10:30:15)
 * - Space-separated timestamps (e.g. 2026-10-26 10:30:15)
 * - Epoch timestamps / numeric values
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return DateTimeUtils.parseSafely(text, null);
    }
}

