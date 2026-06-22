package com.synapsys.api.agent.infrastructure.persistence.repository;

import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.infrastructure.persistence.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, UUID> {

    boolean existsByPublicKey(String publicKey);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE AgentEntity a
        SET a.firstConnectedAt = COALESCE(a.firstConnectedAt, :when), a.lastActivityAt = :when, a.ipAddress = :ip
        WHERE a.id = :id
        """)
    int markConnected(@Param("id") UUID id, @Param("when") Instant when, @Param("ip") String ip);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE AgentEntity a SET a.lastActivityAt = :lastActivityAt, a.ipAddress = :ip WHERE a.id = :id")
    int updateActivitySnapshot(@Param("id") UUID id, @Param("lastActivityAt") Instant lastActivityAt, @Param("ip") String ip);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE AgentEntity a SET a.status = :revoked, a.revokedAt = :now, a.revokedBy = :revokedBy
        WHERE a.id = :id AND a.revokedAt IS NULL
        """)
    int markRevoked(@Param("id") UUID id, @Param("revokedBy") UUID revokedBy,
                    @Param("now") Instant now, @Param("revoked") AgentLifecycleStatus revoked);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM AgentEntity a WHERE a.id = :id AND a.revokedAt IS NOT NULL")
    int deleteIfRevoked(@Param("id") UUID id);

    @Query("SELECT a FROM AgentEntity a WHERE a.revokedAt IS NULL")
    List<AgentEntity> findAllNonRevoked();

    @Query("SELECT COUNT(a) FROM AgentEntity a WHERE a.revokedAt IS NOT NULL")
    long countRevoked();
}
