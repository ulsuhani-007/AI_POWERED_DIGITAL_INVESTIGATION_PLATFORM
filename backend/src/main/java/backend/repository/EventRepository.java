package backend.repository;

import backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInvestigationCaseIdOrderByEventTimeAsc(Long caseId);
}