package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.UpdateCaseRequest;
import com.cybertrace.m1.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public ResponseEntity<List<CaseDTO>> getAllCases() {
        List<CaseDTO> cases = caseService.getAllCases();
        return ResponseEntity.ok(cases);
    }

    @PostMapping
    public ResponseEntity<CaseDTO> createCase(@Valid @RequestBody CreateCaseRequest request) {
        CaseDTO created = caseService.createCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable("id") String id) {
        CaseDTO caseDto = caseService.getCaseById(id);
        return ResponseEntity.ok(caseDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaseDTO> updateCase(@PathVariable("id") String id,
                                              @Valid @RequestBody UpdateCaseRequest request) {
        CaseDTO updated = caseService.updateCase(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(@PathVariable("id") String id) {
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }
}

