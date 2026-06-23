package com.synapsys.api.agent.infrastructure.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.agent.application.port.in.HandleAgentDisconnectUseCase;
import com.synapsys.api.agent.application.port.in.OpenAgentChallengeUseCase;
import com.synapsys.api.agent.application.port.in.RecordAgentHeartbeatUseCase;
import com.synapsys.api.agent.application.port.in.VerifyAgentHandshakeUseCase;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.VerifyHandshakeCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);
    private static final String ATTR_AGENT_ID = "agentId";
    private static final String ATTR_IP = "ip";
    private static final String ATTR_AUTHENTICATED = "authenticated";

    private final OpenAgentChallengeUseCase openChallenge;
    private final VerifyAgentHandshakeUseCase verifyHandshake;
    private final RecordAgentHeartbeatUseCase recordHeartbeat;
    private final HandleAgentDisconnectUseCase handleDisconnect;
    private final LocalAgentSessions localSessions;
    private final AgentConnectionLimiter connectionLimiter;
    private final ObjectMapper objectMapper;
    private final String nodeId = UUID.randomUUID().toString();

    public AgentWebSocketHandler(OpenAgentChallengeUseCase openChallenge,
                                 VerifyAgentHandshakeUseCase verifyHandshake,
                                 RecordAgentHeartbeatUseCase recordHeartbeat,
                                 HandleAgentDisconnectUseCase handleDisconnect,
                                 LocalAgentSessions localSessions,
                                 AgentConnectionLimiter connectionLimiter,
                                 ObjectMapper objectMapper) {
        this.openChallenge = openChallenge;
        this.verifyHandshake = verifyHandshake;
        this.recordHeartbeat = recordHeartbeat;
        this.handleDisconnect = handleDisconnect;
        this.localSessions = localSessions;
        this.connectionLimiter = connectionLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node;
        try {
            node = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            close(session, CloseStatus.BAD_DATA);
            return;
        }
        switch (node.path("type").asText("")) {
            case "announce" -> onAnnounce(session, node);
            case "verify" -> onVerify(session, node);
            case "heartbeat" -> onHeartbeat(session);
            default -> close(session, CloseStatus.BAD_DATA);
        }
    }

    private void onAnnounce(WebSocketSession session, JsonNode node) throws IOException {
        UUID agentId = parseUuid(node.path("agentId").asText(null));
        if (agentId == null) {
            close(session, CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put(ATTR_AGENT_ID, agentId.toString());
        String nonce = openChallenge.openChallenge(session.getId());
        send(session, Map.of("type", "challenge", "nonce", nonce));
    }

    private void onVerify(WebSocketSession session, JsonNode node) throws IOException {
        String agentIdStr = (String) session.getAttributes().get(ATTR_AGENT_ID);
        if (agentIdStr == null) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        byte[] signature;
        try {
            signature = Base64.getUrlDecoder().decode(node.path("signature").asText(""));
        } catch (IllegalArgumentException e) {
            close(session, CloseStatus.BAD_DATA);
            return;
        }
        UUID agentId = UUID.fromString(agentIdStr);
        String ip = (String) session.getAttributes().get(ATTR_IP);
        try {
            verifyHandshake.verify(new VerifyHandshakeCommand(agentId, session.getId(), signature), ip, nodeId);
        } catch (AgentException e) {
            log.debug("Handshake rejected for agent {}: {}", agentId, e.getMessage());
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put(ATTR_AUTHENTICATED, Boolean.TRUE);
        localSessions.register(agentId, session);
        send(session, Map.of("type", "connected"));
    }

    private void onHeartbeat(WebSocketSession session) {
        if (!Boolean.TRUE.equals(session.getAttributes().get(ATTR_AUTHENTICATED))) {
            return;
        }
        String agentIdStr = (String) session.getAttributes().get(ATTR_AGENT_ID);
        if (agentIdStr != null) {
            recordHeartbeat.heartbeat(UUID.fromString(agentIdStr));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String ip = (String) session.getAttributes().get(ATTR_IP);
        if (ip != null) {
            connectionLimiter.release(ip);
        }
        String agentIdStr = (String) session.getAttributes().get(ATTR_AGENT_ID);
        if (agentIdStr == null || !Boolean.TRUE.equals(session.getAttributes().get(ATTR_AUTHENTICATED))) {
            return;
        }
        UUID agentId = UUID.fromString(agentIdStr);
        // Flush only when this session is still the live mapping. A stale session closing after the
        // agent reconnected must not unregister or clear the presence of the new session.
        if (!localSessions.unregister(agentId, session)) {
            return;
        }
        try {
            handleDisconnect.disconnect(agentId, ip);
        } catch (Exception e) {
            log.warn("Disconnect flush failed for agent {}", agentId, e);
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void send(WebSocketSession session, Map<String, String> payload) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.debug("Failed to close session {}", session.getId(), e);
        }
    }
}
