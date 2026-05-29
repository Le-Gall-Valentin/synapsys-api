package com.synapsys.api.auth.domain.model;

import java.util.UUID;

public record ConfirmTotpCommand(UUID userId, String code) {}