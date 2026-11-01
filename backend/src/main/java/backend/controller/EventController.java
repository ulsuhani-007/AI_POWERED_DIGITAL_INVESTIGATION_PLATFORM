
package backend.controller;

import backend.model.Event;
import backend.model.Case;
import backend.repository.EventRepository;
import backend.repository.CaseRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final CaseRepository caseRepository;

    public EventController(
            EventRepository eventRepository,
            CaseRepository caseRepository) {
        this.eventRepository = eventRepository;
        this.caseRepository = caseRepository;
    }

    @PostMapping
    public Event addEvent(@Valid @RequestBody Event event) {
        if (event.getInvestigationCase() == null ||
                event.getInvestigationCase().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Case ID is required");
        }

        Long caseId = event.getInvestigationCase().getId();

        Case investigationCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case not found"));

        event.setInvestigationCase(investigationCase);
        return eventRepository.save(event);
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));
    }

    @GetMapping("/case/{caseId}")
    public List<Event> getEventsByCaseId(@PathVariable Long caseId) {
        return eventRepository
                .findByInvestigationCaseIdOrderByEventTimeAsc(caseId);
    }

    @PutMapping("/{id}")
    public Event updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody Event updatedEvent) {

        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        existingEvent.setTitle(updatedEvent.getTitle());
        existingEvent.setDescription(updatedEvent.getDescription());
        existingEvent.setEventTime(updatedEvent.getEventTime());

        return eventRepository.save(existingEvent);
    }

    @DeleteMapping("/{id}")
    public String deleteEvent(@PathVariable Long id) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"));

        eventRepository.delete(existingEvent);
        return "Event deleted successfully";
    }
}
