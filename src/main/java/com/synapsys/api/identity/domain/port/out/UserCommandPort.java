package com.synapsys.api.identity.domain.port.out;

import com.synapsys.api.identity.domain.model.CreateUserProfileCommand;
import java.util.UUID;

public interface UserCommandPort {
    UUID createProfile(CreateUserProfileCommand command);
    void deactivate(UUID userId);
}