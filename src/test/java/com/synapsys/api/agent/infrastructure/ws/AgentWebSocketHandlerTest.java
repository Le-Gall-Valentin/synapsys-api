package com.synapsys.api.agent.infrastructure.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.agent.application.port.in.HandleAgentDisconnectUseCase;
import com.synapsys.api.agent.application.port.in.OpenAgentChallengeUseCase;
import com.synapsys.api.agent.application.port.in.RecordAgentHeartbeatUseCase;
import com.synapsys.api.agent.application.port.in.VerifyAgentHandshakeUseCase;
import com.synapsys.api.agent.domain.model.AgentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentWebSocketHandlerTest {

    @Mock OpenAgentChallengeUseCase openChallenge;
    @Mock VerifyAgentHandshakeUseCase verifyHandshake;
    @Mock RecordAgentHeartbeatUseCase recordHeartbeat;
    @Mock HandleAgentDisconnectUseCase handleDisconnect;
    @Mock LocalAgentSessions localSessions;
    @Mock AgentConnectionLimiter connectionLimiter;
    @Mock WebSocketSession session;

    private AgentWebSocketHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> attributes = new HashMap<>();
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new AgentWebSocketHandler(openChallenge, verifyHandshake, recordHeartbeat,
            handleDisconnect, localSessions, connectionLimiter, objectMapper);
        lenient().when(session.getId()).thenReturn("conn-1");
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(session.isOpen()).thenReturn(true);
    }

    private TextMessage msg(String json) { return new TextMessage(json); }

    @Test
    void announce_issuesChallengeAndStoresAgentId() throws Exception {
        when(openChallenge.openChallenge("conn-1")).thenReturn("nonce-xyz");

        handler.handleTextMessage(session, msg("{\"type\":\"announce\",\"agentId\":\"" + agentId + "\"}"));

        assertThat(attributes).containsEntry("agentId", agentId.toString());
        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(sent.capture());
        JsonNode out = objectMapper.readTree(sent.getValue().getPayload());
        assertThat(out.get("type").asText()).isEqualTo("challenge");
        assertThat(out.get("nonce").asText()).isEqualTo("nonce-xyz");
    }

    @Test
    void verify_success_registersSessionAndSendsConnected() throws Exception {
        attributes.put("agentId", agentId.toString());
        attributes.put("ip", "1.2.3.4");

        handler.handleTextMessage(session, msg("{\"type\":\"verify\",\"signature\":\"AAAA\"}"));

        verify(verifyHandshake).verify(any(), eq("1.2.3.4"), any());
        verify(localSessions).register(eq(agentId), eq(session));
        assertThat(attributes).containsEntry("authenticated", Boolean.TRUE);
    }

    @Test
    void verify_failure_closesSession() throws Exception {
        attributes.put("agentId", agentId.toString());
        attributes.put("ip", "1.2.3.4");
        doThrow(new AgentException.HandshakeFailed()).when(verifyHandshake).verify(any(), any(), any());

        handler.handleTextMessage(session, msg("{\"type\":\"verify\",\"signature\":\"AAAA\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(localSessions, never()).register(any(), any());
    }

    @Test
    void verify_beforeAnnounce_closesSession() throws Exception {
        handler.handleTextMessage(session, msg("{\"type\":\"verify\",\"signature\":\"AAAA\"}"));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(verifyHandshake, never()).verify(any(), any(), any());
    }

    @Test
    void heartbeat_whenAuthenticated_refreshesPresence() throws Exception {
        attributes.put("agentId", agentId.toString());
        attributes.put("authenticated", Boolean.TRUE);

        handler.handleTextMessage(session, msg("{\"type\":\"heartbeat\"}"));

        verify(recordHeartbeat).heartbeat(agentId);
    }

    @Test
    void heartbeat_whenNotAuthenticated_isIgnored() throws Exception {
        attributes.put("agentId", agentId.toString());
        handler.handleTextMessage(session, msg("{\"type\":\"heartbeat\"}"));
        verify(recordHeartbeat, never()).heartbeat(any());
    }

    @Test
    void afterConnectionClosed_whenAuthenticated_flushesAndUnregisters() {
        attributes.put("agentId", agentId.toString());
        attributes.put("authenticated", Boolean.TRUE);
        attributes.put("ip", "1.2.3.4");
        when(localSessions.unregister(agentId, session)).thenReturn(true);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(connectionLimiter).release("1.2.3.4");
        verify(localSessions).unregister(agentId, session);
        verify(handleDisconnect).disconnect(agentId, "1.2.3.4");
    }

    @Test
    void afterConnectionClosed_staleSession_releasesSlotButDoesNotFlush() {
        attributes.put("agentId", agentId.toString());
        attributes.put("authenticated", Boolean.TRUE);
        attributes.put("ip", "1.2.3.4");
        when(localSessions.unregister(agentId, session)).thenReturn(false); // agent already reconnected

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(connectionLimiter).release("1.2.3.4");
        verify(handleDisconnect, never()).disconnect(any(), any());
    }

    @Test
    void afterConnectionClosed_unauthenticated_stillReleasesSlot() {
        attributes.put("ip", "1.2.3.4");

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(connectionLimiter).release("1.2.3.4");
        verify(localSessions, never()).unregister(any(), any());
        verify(handleDisconnect, never()).disconnect(any(), any());
    }
}
