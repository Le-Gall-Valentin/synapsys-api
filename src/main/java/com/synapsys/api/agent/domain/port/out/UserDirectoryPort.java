package com.synapsys.api.agent.domain.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Resolves usernames for a set of user ids. ACL boundary towards the identity BC. */
public interface UserDirectoryPort {
    /** Returns id → username for the given ids. Unknown or deleted ids are absent from the map. */
    Map<UUID, String> usernamesByIds(Collection<UUID> ids);
}