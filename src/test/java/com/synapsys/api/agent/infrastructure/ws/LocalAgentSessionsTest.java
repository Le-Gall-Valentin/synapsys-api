package com.synapsys.api.agent.infrastructure.ws;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void unregister_removesMatchingSession_soCloseDoesNothing() throws Exception {
        LocalAgentSessions sessions = new LocalAgentSessions();
        UUID agentId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        sessions.register(agentId, session);

        assertThat(sessions.unregister(agentId, session)).isTrue();

        sessions.close(agentId);
        verify(session, never()).close(any());
    }

    @Test
    void unregister_staleSession_doesNotEvictTheReconnectedOne() throws Exception {
        LocalAgentSessions sessions = new LocalAgentSessions();
        UUID agentId = UUID.randomUUID();
        WebSocketSession stale = mock(WebSocketSession.class);
        WebSocketSession current = mock(WebSocketSession.class);
        when(current.isOpen()).thenReturn(true);
        sessions.register(agentId, stale);
        sessions.register(agentId, current); // agent reconnected: current supersedes stale

        // The stale session closing late must not remove the current mapping...
        assertThat(sessions.unregister(agentId, stale)).isFalse();

        // ...so a revocation still reaches the live connection.
        sessions.close(agentId);
        verify(current).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void register_supersedingSession_closesThePreviousOne() throws Exception {
        LocalAgentSessions sessions = new LocalAgentSessions();
        UUID agentId = UUID.randomUUID();
        WebSocketSession previous = mock(WebSocketSession.class);
        WebSocketSession next = mock(WebSocketSession.class);
        when(previous.isOpen()).thenReturn(true);

        sessions.register(agentId, previous);
        sessions.register(agentId, next);

        verify(previous).close(CloseStatus.POLICY_VIOLATION);
    }
}