package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.application.port.in.GetCurrentUserUseCase;
import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class GetCurrentUserHandler implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(IdentityException.UserNotFound::new);
        if (!user.isActive()) {
            throw new IdentityException.UserNotActive();
        }
        return user;
    }
}