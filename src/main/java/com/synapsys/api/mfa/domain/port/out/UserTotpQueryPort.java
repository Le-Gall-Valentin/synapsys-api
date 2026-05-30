package com.synapsys.api.mfa.domain.port.out;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import java.util.Optional;
import java.util.UUID;

public interface UserTotpQueryPort {
    Optional<UserTotpProfile> findById(UUID userId);
}