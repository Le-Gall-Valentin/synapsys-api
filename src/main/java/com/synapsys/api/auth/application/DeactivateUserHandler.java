package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.in.DeactivateUserUseCase;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.auth.domain.service.RoleDeactivationPolicy;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeactivateUserHandler implements DeactivateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeactivateUserHandler.class);

    private final UserRepository userRepository;

    public DeactivateUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void deactivate(UUID targetUserId, UUID callerId, Role callerRole) {
        if (targetUserId.equals(callerId)) {
            throw new AuthException.InsufficientPermissions();
        }
        User target = userRepository.findById(targetUserId)
            .orElseThrow(AuthException.UserNotFound::new);
        if (!RoleDeactivationPolicy.canDeactivate(callerRole, target.role())) {
            throw new AuthException.InsufficientPermissions();
        }
        userRepository.deactivate(targetUserId);
        log.info("User {} deactivated by caller {} with role {}", targetUserId, callerId, callerRole);
    }
}