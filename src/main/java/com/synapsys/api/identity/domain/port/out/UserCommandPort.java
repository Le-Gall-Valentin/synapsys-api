package com.synapsys.api.identity.domain.port.out;

import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import com.synapsys.api.identity.domain.model.User;
import java.util.UUID;

public interface UserCommandPort {
    User createProfile(CreateUserProfileCommand command);
    void deactivate(UUID userId);
    void updateProfile(UUID userId, String username, String email);
}