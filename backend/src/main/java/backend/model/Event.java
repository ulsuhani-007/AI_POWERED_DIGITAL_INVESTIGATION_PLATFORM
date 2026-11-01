package backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Event title cannot be empty")
    private String title;
    private String description;
    private LocalDateTime eventTime;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private Case investigationCase;

    public Event() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public Case getInvestigationCase() {
        return investigationCase;
    }

    public void setInvestigationCase(Case investigationCase) {
        this.investigationCase = investigationCase;
    }
}
