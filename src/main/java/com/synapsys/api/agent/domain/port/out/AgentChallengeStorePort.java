package com.synapsys.api.agent.domain.port.out;

import java.util.Optional;

public interface AgentChallengeStorePort {
    /** Generates a single-use nonce for the connection, stores it with a short TTL, returns it (base64url). */
    String issueChallenge(String connectionId);
    /** Returns and atomically removes the nonce for the connection, or empty if expired/absent. */
    Optional<String> consumeChallenge(String connectionId);
}
