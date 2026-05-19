package com.synapsys.api.infrastructure.ratelimit;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ClientIpResolver {

    private final List<String> trustedProxies;

    ClientIpResolver(SynapsysProperties properties) {
        var rateLimit = properties.rateLimit();
        this.trustedProxies = rateLimit != null
            ? rateLimit.trustedProxies().stream().filter(s -> !s.isBlank()).toList()
            : List.of();
    }

    String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}