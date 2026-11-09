package com.cybertrace.m1.repository;

import com.cybertrace.m1.entity.NormalizedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormalizedEventRepository extends JpaRepository<NormalizedEventEntity, String> {

    List<NormalizedEventEntity> findByCaseEntity_IdOrderByTimestampAsc(String caseId);

    long countByCaseEntity_Id(String caseId);

    void deleteBySourceEvidenceId(String sourceEvidenceId);
}

