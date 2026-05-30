package com.synapsys.api.auth.domain.model;

import java.util.UUID;

public record SetupTotpCommand(UUID userId) {}