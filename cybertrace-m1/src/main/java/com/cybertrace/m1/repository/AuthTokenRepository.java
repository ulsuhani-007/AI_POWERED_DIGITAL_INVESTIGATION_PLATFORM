package com.cybertrace.m1.repository;

import com.cybertrace.m1.entity.AuthTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthTokenEntity, String> {

    Optional<AuthTokenEntity> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Query("UPDATE AuthTokenEntity t SET t.revoked = true WHERE t.user.id = :userId")
    void revokeAllForUser(@Param("userId") String userId);
}

