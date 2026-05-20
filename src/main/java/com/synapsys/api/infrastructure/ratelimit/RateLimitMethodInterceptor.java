package com.synapsys.api.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Locale;

@Component
class RateLimitMethodInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitMethodInterceptor.class);

    private final AttemptTracker tracker;
    private final ClientIpResolver ipResolver;

    RateLimitMethodInterceptor(AttemptTracker tracker, ClientIpResolver ipResolver) {
        this.tracker = tracker;
        this.ipResolver = ipResolver;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RateLimiting annotation = invocation.getMethod().getAnnotation(RateLimiting.class);
        if (annotation == null) return invocation.proceed();

        HttpServletRequest request = currentRequest();
        String ip = ipResolver.resolve(request);

        if (tracker.isLimitExceeded("ip:" + ip, annotation.ipMax(), annotation.ipWindowSeconds())) {
            log.warn("IP rate limit exceeded for {}", ip);
            throw new RateLimitExceededException();
        }

        if (annotation.byUsername()) {
            String username = extractUsername(invocation.getArguments(), annotation.usernameField());
            if (username != null && tracker.isLimitExceeded(
                    "user:" + username.toLowerCase(Locale.ROOT),
                    annotation.usernameMax(), annotation.usernameWindowSeconds())) {
                log.warn("Username rate limit exceeded for user: {}", username);
                throw new RateLimitExceededException();
            }
        }

        return invocation.proceed();
    }

    private String extractUsername(Object[] args, String fieldName) {
        for (Object arg : args) {
            if (arg == null) continue;
            try {
                // Works for records (accessor name = field name) and JavaBeans (getUsername)
                Method accessor = arg.getClass().getMethod(fieldName);
                Object value = accessor.invoke(arg);
                return value instanceof String s ? s : null;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No current HTTP request in context");
        return attrs.getRequest();
    }
}