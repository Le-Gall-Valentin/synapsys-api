package com.synapsys.api.agent.infrastructure.config;

import com.synapsys.api.agent.infrastructure.ws.AgentHandshakeInterceptor;
import com.synapsys.api.agent.infrastructure.ws.AgentWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class AgentWebSocketConfig implements WebSocketConfigurer {

    private final AgentWebSocketHandler handler;
    private final AgentHandshakeInterceptor interceptor;
    private final AgentProperties properties;

    public AgentWebSocketConfig(AgentWebSocketHandler handler, AgentHandshakeInterceptor interceptor,
                                AgentProperties properties) {
        this.handler = handler;
        this.interceptor = interceptor;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, properties.websocketPath()).addInterceptors(interceptor);
        // The SPA fallback controller (RequestMappingHandlerMapping, order 0) matches extensionless
        // paths like /ws/agents. Give the WS handler mapping higher precedence so the upgrade reaches it.
        if (registry instanceof ServletWebSocketHandlerRegistry servletRegistry) {
            servletRegistry.setOrder(-1);
        }
    }
}
