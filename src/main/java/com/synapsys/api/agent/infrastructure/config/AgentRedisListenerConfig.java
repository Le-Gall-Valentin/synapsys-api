package com.synapsys.api.agent.infrastructure.config;

import com.synapsys.api.agent.infrastructure.redis.AgentConnectionRegistry;
import com.synapsys.api.agent.infrastructure.ws.AgentRevocationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class AgentRedisListenerConfig {

    @Bean
    RedisMessageListenerContainer agentRevocationListenerContainer(RedisConnectionFactory connectionFactory,
                                                                   AgentRevocationSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(AgentConnectionRegistry.REVOKE_CHANNEL));
        return container;
    }
}
