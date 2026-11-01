
package backend.controller;

import backend.model.Case;
import backend.repository.CaseRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRepository caseRepository;

    public CaseController(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @PostMapping
    public Case createCase(@Valid @RequestBody Case newCase) {
        return caseRepository.save(newCase);
    }

    @GetMapping
    public List<Case> getAllCases() {
        return caseRepository.findAll();
    }
    @GetMapping("/{id}")
    public Case getCaseById(@PathVariable Long id) {
        return caseRepository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Case not found"));
    }
   @PutMapping("/{id}")
    public Case updateCase(@PathVariable Long id, @Valid @RequestBody Case updatedCase) {
        Case existingCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case not found"));

        existingCase.setTitle(updatedCase.getTitle());
        existingCase.setDescription(updatedCase.getDescription());

        return caseRepository.save(existingCase);
    }

    @DeleteMapping("/{id}")
    public void deleteCase(@PathVariable Long id) {
        Case existingCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case not found"));

        caseRepository.delete(existingCase);
    }
}
    

