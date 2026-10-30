package com.investigation.service;

import com.investigation.model.AnalysisResultModel;
import com.investigation.model.DashboardStats;

public interface AnalysisService {
    AnalysisResultModel getAnalysisByCase(String caseId) throws Exception;
    DashboardStats getDashboardStats() throws Exception;
}
