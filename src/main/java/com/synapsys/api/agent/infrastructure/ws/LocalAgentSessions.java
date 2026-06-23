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
        sessions.put(agentId, session);
    }

    public void unregister(UUID agentId) {
        sessions.remove(agentId);
    }

    public void close(UUID agentId) {
        WebSocketSession session = sessions.remove(agentId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.debug("Failed to close session for revoked agent {}", agentId, e);
            }
        }
    }
}
