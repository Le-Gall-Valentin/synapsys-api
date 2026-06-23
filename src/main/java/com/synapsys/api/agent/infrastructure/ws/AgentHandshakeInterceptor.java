package com.synapsys.api.agent.infrastructure.ws;

import com.synapsys.api.infrastructure.ratelimit.ClientIpResolver;
import org.springframework.http.HttpStatus;
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
    private final AgentConnectionLimiter connectionLimiter;

    public AgentHandshakeInterceptor(ClientIpResolver ipResolver, AgentConnectionLimiter connectionLimiter) {
        this.ipResolver = ipResolver;
        this.connectionLimiter = connectionLimiter;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }
        String ip = ipResolver.resolve(servletRequest.getServletRequest());
        if (!connectionLimiter.tryAcquire(ip)) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }
        attributes.put(IP_ATTRIBUTE, ip);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // The upgrade failed after a slot was reserved; afterConnectionClosed will not fire,
        // so release the reserved slot here to avoid leaking the per-IP budget.
        if (exception != null && request instanceof ServletServerHttpRequest servletRequest) {
            connectionLimiter.release(ipResolver.resolve(servletRequest.getServletRequest()));
        }
    }
}
