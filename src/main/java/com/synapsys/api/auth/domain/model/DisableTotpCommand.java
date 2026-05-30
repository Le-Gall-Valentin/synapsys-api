package com.synapsys.api.auth.domain.model;

import java.util.UUID;

public record DisableTotpCommand(UUID userId, String code) {}