package com.synapsys.api.agent.infrastructure.ws;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalAgentSessionsTest {

    @Test
    void close_closesRegisteredOpenSession() throws Exception {
        LocalAgentSessions sessions = new LocalAgentSessions();
        UUID agentId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        sessions.register(agentId, session);

        sessions.close(agentId);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void close_unknownAgent_isNoOp() {
        LocalAgentSessions sessions = new LocalAgentSessions();
        sessions.close(UUID.randomUUID()); // must not throw
    }

    @Test
    void unregister_removesSession_soCloseDoesNothing() throws Exception {
        LocalAgentSessions sessions = new LocalAgentSessions();
        UUID agentId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        sessions.register(agentId, session);
        sessions.unregister(agentId);

        sessions.close(agentId);

        verify(session, never()).close(CloseStatus.POLICY_VIOLATION);
    }
}
