package com.synapsys.api.agent.infrastructure.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.agent.domain.port.out.AgentPresencePort;
import com.synapsys.api.agent.infrastructure.persistence.entity.AgentEntity;
import com.synapsys.api.agent.infrastructure.persistence.entity.EnrollmentTokenEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.AgentJpaRepository;
import com.synapsys.api.agent.infrastructure.persistence.repository.EnrollmentTokenJpaRepository;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "synapsys.jwt.secret=integration-test-secret-at-least-32-chars!",
    "synapsys.seed.username=it-admin",
    "synapsys.seed.email=it-admin@test.local",
    "synapsys.seed.password=integration-test-seed-password",
    "synapsys.cors.allowed-origins=",
    "synapsys.encryption.secret=integration-test-encryption-secret-32chars!",
    "spring.jpa.hibernate.ddl-auto=none"
})
class AgentWebSocketIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container @ServiceConnection @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @LocalServerPort int port;
    @Autowired AgentJpaRepository agentRepository;
    @Autowired EnrollmentTokenJpaRepository tokenRepository;
    @Autowired UserIdentityJpaRepository userRepository;
    @Autowired AgentPresencePort presence;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KeyPair keyPair;
    private UUID agentId;

    @BeforeEach
    void setUp() throws Exception {
        agentRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        UserIdentityEntity u = new UserIdentityEntity();
        u.setUsername("ws-admin");
        u.setEmail("ws-admin@test.com");
        u.setRole(Role.ADMIN);
        UUID adminId = userRepository.saveAndFlush(u).getId();
        UUID tokenId = tokenRepository.saveAndFlush(new EnrollmentTokenEntity(
            "web-01", "h", java.time.Instant.now().plusSeconds(3600), adminId)).getId();

        keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String pkB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(encodeRaw((EdECPublicKey) keyPair.getPublic()));
        AgentEntity agent = new AgentEntity("web-01", pkB64, "fp-ws", tokenId, adminId);
        agentId = agentRepository.saveAndFlush(agent).getId();
    }

    @Test
    void fullHandshake_marksAgentPresent() throws Exception {
        BlockingQueue<String> inbound = new LinkedBlockingQueue<>();
        WebSocketSession session = new StandardWebSocketClient()
            .execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession s, TextMessage m) {
                    inbound.add(m.getPayload());
                }
            }, new org.springframework.web.socket.WebSocketHttpHeaders(),
            URI.create("ws://localhost:" + port + "/ws/agents")).get(5, TimeUnit.SECONDS);

        session.sendMessage(new TextMessage(
            objectMapper.writeValueAsString(java.util.Map.of("type", "announce", "agentId", agentId.toString()))));

        JsonNode challenge = objectMapper.readTree(inbound.poll(5, TimeUnit.SECONDS));
        assertThat(challenge.get("type").asText()).isEqualTo("challenge");

        byte[] sig = sign(keyPair, challenge.get("nonce").asText().getBytes(StandardCharsets.UTF_8));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            java.util.Map.of("type", "verify", "signature", Base64.getUrlEncoder().withoutPadding().encodeToString(sig)))));

        JsonNode connected = objectMapper.readTree(inbound.poll(5, TimeUnit.SECONDS));
        assertThat(connected.get("type").asText()).isEqualTo("connected");
        assertThat(presence.isPresent(agentId)).isTrue();

        session.close(CloseStatus.NORMAL);
    }

    private static byte[] sign(KeyPair kp, byte[] msg) throws Exception {
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(kp.getPrivate());
        s.update(msg);
        return s.sign();
    }

    static byte[] encodeRaw(EdECPublicKey key) {
        byte[] y = key.getPoint().getY().toByteArray();
        byte[] le = new byte[32];
        for (int i = 0; i < y.length && i < 32; i++) {
            le[i] = y[y.length - 1 - i];
        }
        if (key.getPoint().isXOdd()) {
            le[31] |= (byte) 0x80;
        }
        return le;
    }
}