package com.cybertrace.m1.repository;

import com.cybertrace.m1.entity.EvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceRepository extends JpaRepository<EvidenceEntity, String> {

    List<EvidenceEntity> findByCaseEntity_IdOrderByUploadedAtDesc(String caseId);

    Optional<EvidenceEntity> findByIdAndCaseEntity_Id(String id, String caseId);

    boolean existsByCaseEntity_IdAndHashSha256(String caseId, String hashSha256);

    long countByCaseEntity_Id(String caseId);
}

