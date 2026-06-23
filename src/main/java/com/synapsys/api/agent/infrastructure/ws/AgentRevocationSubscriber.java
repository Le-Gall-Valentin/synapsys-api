package com.synapsys.api.agent.infrastructure.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class AgentRevocationSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(AgentRevocationSubscriber.class);

    private final LocalAgentSessions sessions;

    public AgentRevocationSubscriber(LocalAgentSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8).trim();
        try {
            sessions.close(UUID.fromString(body));
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring revoke message with invalid agent id: {}", body);
        }
    }
}
