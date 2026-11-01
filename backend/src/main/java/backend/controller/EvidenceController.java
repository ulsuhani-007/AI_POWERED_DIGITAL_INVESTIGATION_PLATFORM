package backend.controller;

import backend.model.Evidence;
import backend.repository.EvidenceRepository;
import backend.model.Case;
import backend.repository.CaseRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceRepository evidenceRepository;
    private final CaseRepository caseRepository;

    public EvidenceController(
        EvidenceRepository evidenceRepository,
        CaseRepository caseRepository) {
      this.evidenceRepository = evidenceRepository;
      this.caseRepository = caseRepository;
    }

    @PostMapping
    public Evidence addEvidence(@Valid @RequestBody Evidence evidence) {
      Long caseId = evidence.getInvestigationCase().getId();

      Case investigationCase = caseRepository.findById(caseId)
              .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                      org.springframework.http.HttpStatus.NOT_FOUND,
                      "Case not found"));

      evidence.setInvestigationCase(investigationCase);

      return evidenceRepository.save(evidence);
    }

    @GetMapping
    public List<Evidence> getAllEvidence() {
        return evidenceRepository.findAll();
    }

    @GetMapping("/{id}")
    public Evidence getEvidenceById(@PathVariable Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Evidence not found"));
    }
    @PutMapping("/{id}")
    public Evidence updateEvidence(@PathVariable Long id, @Valid @RequestBody Evidence updatedEvidence){
      Evidence existingEvidence = evidenceRepository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Evidence not found"));

        existingEvidence.setTitle(updatedEvidence.getTitle());
        existingEvidence.setDescription(updatedEvidence.getDescription());
        existingEvidence.setFilePath(updatedEvidence.getFilePath());

        return evidenceRepository.save(existingEvidence);
  }
  @DeleteMapping("/{id}")
  public String deleteEvidence(@PathVariable Long id) {
      Evidence existingEvidence = evidenceRepository.findById(id)
              .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                      org.springframework.http.HttpStatus.NOT_FOUND,
                      "Evidence not found"));

        evidenceRepository.delete(existingEvidence);

        return "Evidence deleted successfully";
  }
  @GetMapping("/case/{caseId}")
  public List<Evidence> getEvidenceByCaseId(@PathVariable Long caseId) {
      return evidenceRepository.findByInvestigationCaseId(caseId);
  }
}