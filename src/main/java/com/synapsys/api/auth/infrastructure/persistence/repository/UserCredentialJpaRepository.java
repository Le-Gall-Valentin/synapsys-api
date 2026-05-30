package com.synapsys.api.auth.infrastructure.persistence.repository;

import com.synapsys.api.auth.infrastructure.persistence.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredentialEntity, UUID> {}