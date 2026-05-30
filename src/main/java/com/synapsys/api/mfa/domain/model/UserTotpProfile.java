package com.synapsys.api.mfa.domain.model;

import com.synapsys.api.shared.model.Role;
import java.util.UUID;

public record UserTotpProfile(UUID id, Role role, boolean totpEnabled, String totpSecret) {}