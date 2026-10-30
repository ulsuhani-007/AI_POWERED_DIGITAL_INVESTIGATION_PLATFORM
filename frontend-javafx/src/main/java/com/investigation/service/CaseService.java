package com.investigation.service;

import com.investigation.model.CaseModel;

import java.util.List;

public interface CaseService {
    List<CaseModel> getAllCases() throws Exception;
    CaseModel getCaseById(String id) throws Exception;
    CaseModel createCase(CaseModel caseModel) throws Exception;
    CaseModel updateCase(CaseModel caseModel) throws Exception;
    boolean deleteCase(String id) throws Exception;
}
