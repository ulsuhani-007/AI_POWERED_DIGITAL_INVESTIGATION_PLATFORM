package com.cybertrace.m1.repository;

import com.cybertrace.m1.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, String> {

    Optional<CaseEntity> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);
}

