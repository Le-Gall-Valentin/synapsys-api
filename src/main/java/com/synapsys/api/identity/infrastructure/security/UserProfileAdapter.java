package com.synapsys.api.identity.infrastructure.security;

import com.synapsys.api.authentication.domain.model.UserProfile;
import com.synapsys.api.authentication.domain.port.out.UserProfilePort;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserProfileAdapter implements UserProfilePort {

    private final UserRepository userRepository;

    public UserProfileAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(u -> new UserProfile(u.id(), u.username(), u.email(), u.isActive(), u.role()));
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return userRepository.findById(id)
                .map(u -> new UserProfile(u.id(), u.username(), u.email(), u.isActive(), u.role()));
    }
}