package com.synapsys.api.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
public class UserCredentialEntity {

    @Id
    private UUID userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}