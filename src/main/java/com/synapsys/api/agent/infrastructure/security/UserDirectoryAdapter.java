package com.synapsys.api.agent.infrastructure.security;

import com.synapsys.api.agent.domain.port.out.UserDirectoryPort;
import com.synapsys.api.identity.application.port.in.FindUserUseCase;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserDirectoryAdapter implements UserDirectoryPort {

    private final FindUserUseCase findUser;

    public UserDirectoryAdapter(FindUserUseCase findUser) {
        this.findUser = findUser;
    }

    @Override
    public Map<UUID, String> usernamesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return findUser.findByIds(ids).stream()
            .filter(u -> u.username() != null)
            .collect(Collectors.toMap(u -> u.id(), u -> u.username()));
    }
}