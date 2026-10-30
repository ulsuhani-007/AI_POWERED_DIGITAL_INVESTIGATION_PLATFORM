package com.investigation.service;

import com.investigation.model.EventModel;

import java.util.List;

public interface EventService {
    List<EventModel> getEventsByCase(String caseId) throws Exception;
}
