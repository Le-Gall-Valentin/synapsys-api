package com.synapsys.api.agent.infrastructure.ws;

import com.synapsys.api.infrastructure.ratelimit.ClientIpResolver;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class AgentHandshakeInterceptor implements HandshakeInterceptor {

    static final String IP_ATTRIBUTE = "ip";

    private final ClientIpResolver ipResolver;

    public AgentHandshakeInterceptor(ClientIpResolver ipResolver) {
        this.ipResolver = ipResolver;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            attributes.put(IP_ATTRIBUTE, ipResolver.resolve(servletRequest.getServletRequest()));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
