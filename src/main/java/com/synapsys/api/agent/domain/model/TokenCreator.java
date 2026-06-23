package com.synapsys.api.agent.domain.model;

import java.util.UUID;

/** Creator of an enrollment token. {@code username} is null when the user no longer exists. */
public record TokenCreator(UUID id, String username) {}