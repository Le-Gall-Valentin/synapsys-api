package com.synapsys.api.identity.application.service;

import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.Role;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class UserCredentialsService {

    private final UserRepository userRepository;

    public UserCredentialsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record UserInfo(UUID id, String username, String email, boolean isActive, Role role, Instant createdAt) {}

    public Optional<UserInfo> findByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(u -> new UserInfo(u.id(), u.username(), u.email(), u.isActive(), u.role(), u.createdAt()));
    }

    public Optional<UserInfo> findById(UUID id) {
        return userRepository.findById(id)
            .map(u -> new UserInfo(u.id(), u.username(), u.email(), u.isActive(), u.role(), u.createdAt()));
    }
}