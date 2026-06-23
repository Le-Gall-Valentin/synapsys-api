package com.synapsys.api.agent.infrastructure.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LocalAgentSessions {

    private static final Logger log = LoggerFactory.getLogger(LocalAgentSessions.class);

    private final ConcurrentMap<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(UUID agentId, WebSocketSession session) {
        WebSocketSession previous = sessions.put(agentId, session);
        // A reconnecting agent supersedes its previous session: close the stale one so it
        // does not linger half-open. Its later afterConnectionClosed is a no-op (identity check).
        if (previous != null && previous != session) {
            closeQuietly(previous);
        }
    }

    /**
     * Removes the mapping only if {@code session} is still the registered one.
     * Returns {@code true} when this session was the live mapping. A stale session closing
     * after the agent already reconnected returns {@code false} and must not evict the new session.
     */
    public boolean unregister(UUID agentId, WebSocketSession session) {
        return sessions.remove(agentId, session);
    }

    public void close(UUID agentId) {
        WebSocketSession session = sessions.remove(agentId);
        if (session != null) {
            closeQuietly(session);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.debug("Failed to close session {}", session.getId(), e);
        }
    }
}
