package com.synapsys.api.identity.infrastructure.persistence.repository;

import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityJpaRepository extends JpaRepository<UserIdentityEntity, UUID> {
    Optional<UserIdentityEntity> findByUsername(String username);
    Optional<UserIdentityEntity> findByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.active = false WHERE u.id = :id")
    void deactivateById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE UserIdentityEntity u SET u.username = :username, u.email = :email WHERE u.id = :id")
    void updateProfile(@Param("id") UUID id,
                       @Param("username") String username,
                       @Param("email") String email);
}