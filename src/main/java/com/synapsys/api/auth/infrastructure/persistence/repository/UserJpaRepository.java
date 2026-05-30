package com.synapsys.api.auth.infrastructure.persistence.repository;

import com.synapsys.api.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserEntity u SET u.active = false WHERE u.id = :id")
    void deactivateById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserEntity u SET u.totpSecret = :secret WHERE u.id = :id")
    void saveTotpSecretById(@Param("id") UUID id, @Param("secret") String secret);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserEntity u SET u.totpEnabled = true WHERE u.id = :id")
    void enableTotpById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserEntity u SET u.totpEnabled = false, u.totpSecret = null WHERE u.id = :id")
    void disableTotpById(@Param("id") UUID id);
}