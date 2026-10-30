package com.investigation.service;

import com.investigation.api.ApiClient;
import com.investigation.api.RestAnalysisService;
import com.investigation.api.RestCaseService;
import com.investigation.api.RestEventService;
import com.investigation.api.RestEvidenceService;
import com.investigation.config.AppConfig;
import com.investigation.service.mock.MockAnalysisService;
import com.investigation.service.mock.MockCaseService;
import com.investigation.service.mock.MockDataGenerator;
import com.investigation.service.mock.MockEventService;
import com.investigation.service.mock.MockEvidenceService;

/**
 * Service factory providing either Mock or REST API service implementations
 * based on the active runtime configuration.
 */
public class ServiceFactory {

    private static final ServiceFactory INSTANCE = new ServiceFactory();

    private final ApiClient apiClient;
    private final MockDataGenerator mockDataGenerator;

    private final MockCaseService mockCaseService;
    private final MockEvidenceService mockEvidenceService;
    private final MockEventService mockEventService;
    private final MockAnalysisService mockAnalysisService;

    private final RestCaseService restCaseService;
    private final RestEvidenceService restEvidenceService;
    private final RestEventService restEventService;
    private final RestAnalysisService restAnalysisService;

    private ServiceFactory() {
        this.apiClient = new ApiClient();
        this.mockDataGenerator = new MockDataGenerator();

        this.mockCaseService = new MockCaseService(mockDataGenerator);
        this.mockEvidenceService = new MockEvidenceService(mockDataGenerator);
        this.mockEventService = new MockEventService(mockDataGenerator);
        this.mockAnalysisService = new MockAnalysisService(mockDataGenerator);

        this.restCaseService = new RestCaseService(apiClient);
        this.restEvidenceService = new RestEvidenceService(apiClient);
        this.restEventService = new RestEventService(apiClient);
        this.restAnalysisService = new RestAnalysisService(apiClient);
    }

    public static ServiceFactory getInstance() {
        return INSTANCE;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public MockDataGenerator getMockDataGenerator() {
        return mockDataGenerator;
    }

    public CaseService getCaseService() {
        return AppConfig.getInstance().isMockModeEnabled() ? mockCaseService : restCaseService;
    }

    public EvidenceService getEvidenceService() {
        return AppConfig.getInstance().isMockModeEnabled() ? mockEvidenceService : restEvidenceService;
    }

    public EventService getEventService() {
        return AppConfig.getInstance().isMockModeEnabled() ? mockEventService : restEventService;
    }

    public AnalysisService getAnalysisService() {
        return AppConfig.getInstance().isMockModeEnabled() ? mockAnalysisService : restAnalysisService;
    }
}
