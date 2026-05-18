package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.in.GetCurrentUserUseCase;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;

import java.util.UUID;

@ApplicationService
public class GetCurrentUserHandler implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(AuthException.UserNotFound::new);
        if (!user.isActive()) {
            throw new AuthException.UserNotActive();
        }
        return user;
    }
}