package com.cybertrace.m1.repository;

import com.cybertrace.m1.entity.AnalysisReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReportEntity, String> {

    Optional<AnalysisReportEntity> findByCaseEntity_Id(String caseId);
}

