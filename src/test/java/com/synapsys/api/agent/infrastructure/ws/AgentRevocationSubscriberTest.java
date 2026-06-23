package com.synapsys.api.agent.infrastructure.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRevocationSubscriberTest {

    @Mock LocalAgentSessions sessions;
    @Mock Message message;

    @Test
    void onMessage_closesLocalSessionForAgentId() {
        UUID agentId = UUID.randomUUID();
        when(message.getBody()).thenReturn(agentId.toString().getBytes(StandardCharsets.UTF_8));
        new AgentRevocationSubscriber(sessions).onMessage(message, null);
        verify(sessions).close(agentId);
    }
}
