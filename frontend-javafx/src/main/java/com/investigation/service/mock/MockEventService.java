package com.investigation.service.mock;

import com.investigation.model.EventModel;
import com.investigation.service.EventService;

import java.util.List;

public class MockEventService implements EventService {

    private final MockDataGenerator generator;

    public MockEventService(MockDataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public List<EventModel> getEventsByCase(String caseId) {
        return generator.getEventsByCase(caseId);
    }
}
