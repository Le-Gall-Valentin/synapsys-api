package com.synapsys.api.infrastructure.ratelimit;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitMethodInterceptorTest {

    @Mock AttemptTracker tracker;
    @Mock ClientIpResolver ipResolver;
    @Mock MethodInvocation invocation;

    private RateLimitMethodInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitMethodInterceptor(tracker, ipResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void proceedsWhenIpNotLimited() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("ipOnly"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(tracker.isLimitExceeded(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(invocation.proceed()).thenReturn("ok");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        verify(invocation).proceed();
    }

    @Test
    void throws429WhenIpLimitExceeded() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("ipOnly"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(tracker.isLimitExceeded(startsWith("ip:"), anyInt(), anyInt())).thenReturn(true);

        assertThatThrownBy(() -> interceptor.invoke(invocation))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verify(invocation, never()).proceed();
    }

    @Test
    void throws429WhenUsernameLimitExceeded() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("withUsername"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(tracker.isLimitExceeded(startsWith("ip:"), anyInt(), anyInt())).thenReturn(false);
        when(tracker.isLimitExceeded(startsWith("user:"), anyInt(), anyInt())).thenReturn(true);

        record LoginReq(String username) {}
        when(invocation.getArguments()).thenReturn(new Object[]{ new LoginReq("alice") });

        assertThatThrownBy(() -> interceptor.invoke(invocation))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void usernameIsLowercasedForLimitCheck() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("withUsername"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(tracker.isLimitExceeded(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(invocation.proceed()).thenReturn(null);

        record LoginReq(String username) {}
        when(invocation.getArguments()).thenReturn(new Object[]{ new LoginReq("Alice") });

        interceptor.invoke(invocation);

        verify(tracker).isLimitExceeded(eq("user:alice"), anyInt(), anyInt());
    }

    @Test
    void skipsUsernameCheckWhenArgHasNoField() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("withUsername"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(tracker.isLimitExceeded(startsWith("ip:"), anyInt(), anyInt())).thenReturn(false);
        when(invocation.proceed()).thenReturn(null);
        // Argument has no username() accessor
        when(invocation.getArguments()).thenReturn(new Object[]{ "not-a-request-object" });

        interceptor.invoke(invocation);

        verify(tracker, never()).isLimitExceeded(startsWith("user:"), anyInt(), anyInt());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static Method method(String name) throws NoSuchMethodException {
        return TestEndpoints.class.getMethod(name);
    }

    static class TestEndpoints {
        @RateLimiting
        public void ipOnly() {}

        @RateLimiting(byUsername = true, usernameField = "username")
        public void withUsername() {}
    }
}