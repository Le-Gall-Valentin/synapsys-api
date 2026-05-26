package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.DeactivateUserCommand;
import com.synapsys.api.auth.domain.model.User;
import com.synapsys.api.auth.domain.port.in.DeactivateUserUseCase;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import com.synapsys.api.auth.domain.service.RoleHierarchy;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class DeactivateUserHandler implements DeactivateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeactivateUserHandler.class);

    private final UserRepository userRepository;

    public DeactivateUserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void deactivate(DeactivateUserCommand command) {
        if (command.targetUserId().equals(command.callerId())) {
            throw new AuthException.InsufficientPermissions();
        }
        User target = userRepository.findById(command.targetUserId())
            .orElseThrow(AuthException.UserNotFound::new);
        if (!target.isActive()) {
            throw new AuthException.UserAlreadyInactive();
        }
        if (!RoleHierarchy.canManage(command.callerRole(), target.role())) {
            throw new AuthException.InsufficientPermissions();
        }
        userRepository.deactivate(command.targetUserId());
        log.info("User {} deactivated by caller {} with role {}",
            command.targetUserId(), command.callerId(), command.callerRole());
    }
}