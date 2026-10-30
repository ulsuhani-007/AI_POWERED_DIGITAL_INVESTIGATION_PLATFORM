package com.investigation.service.mock;

import com.investigation.model.AnalysisResultModel;
import com.investigation.model.DashboardStats;
import com.investigation.service.AnalysisService;

public class MockAnalysisService implements AnalysisService {

    private final MockDataGenerator generator;

    public MockAnalysisService(MockDataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public AnalysisResultModel getAnalysisByCase(String caseId) {
        return generator.getAnalysisByCase(caseId);
    }

    @Override
    public DashboardStats getDashboardStats() {
        return generator.getDashboardStats();
    }
}
