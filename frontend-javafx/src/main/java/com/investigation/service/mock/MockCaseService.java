package com.investigation.service.mock;

import com.investigation.model.CaseModel;
import com.investigation.service.CaseService;

import java.util.List;

public class MockCaseService implements CaseService {

    private final MockDataGenerator generator;

    public MockCaseService(MockDataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public List<CaseModel> getAllCases() {
        return generator.getAllCases();
    }

    @Override
    public CaseModel getCaseById(String id) {
        return generator.getCaseById(id);
    }

    @Override
    public CaseModel createCase(CaseModel caseModel) {
        return generator.createCase(caseModel);
    }

    @Override
    public CaseModel updateCase(CaseModel caseModel) {
        return generator.updateCase(caseModel);
    }

    @Override
    public boolean deleteCase(String id) {
        return generator.deleteCase(id);
    }
}
