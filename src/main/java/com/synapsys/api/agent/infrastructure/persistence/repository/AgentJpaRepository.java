package com.synapsys.api.agent.infrastructure.persistence.repository;

import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.infrastructure.persistence.entity.AgentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        WHERE a.id = :id AND a.revokedAt IS NULL
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

    // Filter on the indexed status column (idx_agents_status) rather than revoked_at,
    // so the planner can use the index; status REVOKED <=> revoked_at set (see markRevoked).
    @Query("SELECT a FROM AgentEntity a WHERE a.status = ENROLLED")
    List<AgentEntity> findAllNonRevoked();

    // Pattern pré-échappé par l'adapter ('!' échappe '!', '%' et '_'). ipAddress est null
    // tant que l'agent n'est pas connecté (PENDING) : il ne matchera alors aucun terme.
    @Query("""
        SELECT a FROM AgentEntity a
        WHERE (LOWER(a.serverName) LIKE :pattern ESCAPE '!'
            OR LOWER(a.ipAddress) LIKE :pattern ESCAPE '!')
        """)
    Page<AgentEntity> searchAll(@Param("pattern") String pattern,
                                Pageable pageable);

    @Query("SELECT COUNT(a) FROM AgentEntity a WHERE a.status = REVOKED")
    long countRevoked();
}
