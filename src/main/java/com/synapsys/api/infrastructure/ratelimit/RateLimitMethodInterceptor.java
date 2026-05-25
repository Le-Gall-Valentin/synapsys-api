package com.synapsys.api.infrastructure.ratelimit;

import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Component
class RateLimitMethodInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitMethodInterceptor.class);

    private final RateLimitBucketStore store;
    private final ClientIpResolver ipResolver;

    RateLimitMethodInterceptor(RateLimitBucketStore store, ClientIpResolver ipResolver) {
        this.store = store;
        this.ipResolver = ipResolver;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RateLimiting[] rules = resolveAnnotations(invocation.getMethod());
        if (rules.length == 0) return invocation.proceed();

        HttpServletRequest request = currentRequest();
        String ip = ipResolver.resolve(request);
        String userId = resolveUserId();
        String endpoint = invocation.getMethod().getDeclaringClass().getSimpleName()
            + "." + invocation.getMethod().getName();

        RateLimitHeaders worst = null;

        for (RateLimiting rule : rules) {
            List<String> keys = resolveKeys(rule.mode(), endpoint, ip, userId);
            for (String key : keys) {
                RateLimitBucketStore.BucketResult result = store.tryConsume(key, rule.max(), rule.windowSeconds());
                RateLimitHeaders headers = RateLimitHeaders.from(result, rule.max());
                worst = worst == null ? headers : worst.worstCase(headers);

                if (!result.allowed()) {
                    log.warn("Rate limit exceeded — key={} limit={} window={}s", key, rule.max(), rule.windowSeconds());
                    HttpServletResponse blockedResponse = currentResponse();
                    blockedResponse.setHeader("X-RateLimit-Limit",     String.valueOf(worst.limit()));
                    blockedResponse.setHeader("X-RateLimit-Remaining", "0");
                    blockedResponse.setHeader("X-RateLimit-Reset",     String.valueOf(worst.resetEpochSeconds()));
                    throw new RateLimitExceededException(worst);
                }
            }
        }

        if (worst != null) {
            HttpServletResponse response = currentResponse();
            response.setHeader("X-RateLimit-Limit",     String.valueOf(worst.limit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(worst.remaining()));
            response.setHeader("X-RateLimit-Reset",     String.valueOf(worst.resetEpochSeconds()));
        }

        return invocation.proceed();
    }

    private RateLimiting[] resolveAnnotations(Method method) {
        RateLimitingList container = method.getAnnotation(RateLimitingList.class);
        if (container != null) return container.value();
        RateLimiting single = method.getAnnotation(RateLimiting.class);
        return single != null ? new RateLimiting[]{single} : new RateLimiting[0];
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) return null;
        if (auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUserId().toString();
        }
        return null;
    }

    private List<String> resolveKeys(RateLimitMode mode, String endpoint, String ip, String userId) {
        List<String> keys = new ArrayList<>();
        switch (mode) {
            case IP -> keys.add(endpoint + ":IP:" + ip);
            case USER -> { if (userId != null) keys.add(endpoint + ":USER:" + userId); }
            case IP_AND_USER -> {
                keys.add(endpoint + ":IP:" + ip);
                if (userId != null) keys.add(endpoint + ":USER:" + userId);
            }
        }
        return keys;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No current HTTP request");
        return attrs.getRequest();
    }

    private static HttpServletResponse currentResponse() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No current HTTP request");
        HttpServletResponse response = attrs.getResponse();
        if (response == null) throw new IllegalStateException("No current HTTP response");
        return response;
    }
}