package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.NormalizedEventDTO;
import com.cybertrace.m1.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases/{caseId}/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<NormalizedEventDTO>> getEventsByCase(@PathVariable("caseId") String caseId) {
        List<NormalizedEventDTO> events = eventService.getEventsByCase(caseId);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<List<NormalizedEventDTO>> ingestEvents(
            @PathVariable("caseId") String caseId,
            @RequestBody List<NormalizedEventDTO> events) {
        List<NormalizedEventDTO> savedEvents = eventService.ingestEvents(caseId, events);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvents);
    }
}

