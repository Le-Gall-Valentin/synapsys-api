package com.synapsys.api.infrastructure.ratelimit;

import com.synapsys.api.auth.infrastructure.security.CustomUserDetails;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitMethodInterceptorTest {

    @Mock RateLimitBucketStore store;
    @Mock ClientIpResolver ipResolver;
    @Mock MethodInvocation invocation;

    private RateLimitMethodInterceptor interceptor;
    private MockHttpServletResponse response;

    private static final RateLimitBucketStore.BucketResult ALLOWED =
        new RateLimitBucketStore.BucketResult(true, 9L, 0L);
    private static final RateLimitBucketStore.BucketResult BLOCKED =
        new RateLimitBucketStore.BucketResult(false, 0L, 2_000_000_000L);

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitMethodInterceptor(store, ipResolver);
        response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ── IP mode ──────────────────────────────────────────────────────────

    @Test
    void ipMode_allowed_proceedsAndSetsHeaders() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("ipOnly"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(store.tryConsume(contains(":IP:1.2.3.4"), anyInt(), anyInt())).thenReturn(ALLOWED);
        when(invocation.proceed()).thenReturn("ok");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    void ipMode_blocked_throwsExceptionWithHeaders() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("ipOnly"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(store.tryConsume(contains(":IP:1.2.3.4"), anyInt(), anyInt())).thenReturn(BLOCKED);

        assertThatThrownBy(() -> interceptor.invoke(invocation))
            .isInstanceOf(RateLimitExceededException.class)
            .satisfies(e -> {
                RateLimitExceededException ex = (RateLimitExceededException) e;
                assertThat(ex.getLimit()).isEqualTo(10L);
                assertThat(ex.getRemaining()).isZero();
                assertThat(ex.getRetryAfterSeconds()).isPositive();
            });
        verify(invocation, never()).proceed();
    }

    @Test
    void ipMode_blocked_setsRateLimitHeadersOnResponse() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("ipOnly"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(store.tryConsume(contains(":IP:1.2.3.4"), anyInt(), anyInt())).thenReturn(BLOCKED);

        assertThatThrownBy(() -> interceptor.invoke(invocation))
            .isInstanceOf(RateLimitExceededException.class);

        assertThat(response.getHeader("X-RateLimit-Limit")).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
    }

    // ── USER mode ────────────────────────────────────────────────────────

    @Test
    void userMode_authenticated_usesUserIdInKey() throws Throwable {
        UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        setAuthenticatedUser(userId);

        when(invocation.getMethod()).thenReturn(method("userOnly"));
        when(store.tryConsume(contains(":USER:" + userId), anyInt(), anyInt())).thenReturn(ALLOWED);
        when(invocation.proceed()).thenReturn(null);

        interceptor.invoke(invocation);

        verify(store).tryConsume(contains(":USER:" + userId), anyInt(), anyInt());
        verify(store, never()).tryConsume(contains(":IP:"), anyInt(), anyInt());
    }

    @Test
    void userMode_notAuthenticated_skipsCheckAndProceeds() throws Throwable {
        // No SecurityContext set — principal is absent
        when(invocation.getMethod()).thenReturn(method("userOnly"));
        when(invocation.proceed()).thenReturn(null);

        interceptor.invoke(invocation);

        verify(store, never()).tryConsume(anyString(), anyInt(), anyInt());
    }

    // ── IP_AND_USER mode ─────────────────────────────────────────────────

    @Test
    void ipAndUserMode_bothChecked_bothPass() throws Throwable {
        UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        setAuthenticatedUser(userId);

        when(invocation.getMethod()).thenReturn(method("ipAndUser"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(store.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(ALLOWED);
        when(invocation.proceed()).thenReturn(null);

        interceptor.invoke(invocation);

        verify(store).tryConsume(contains(":IP:1.2.3.4"), anyInt(), anyInt());
        verify(store).tryConsume(contains(":USER:" + userId), anyInt(), anyInt());
    }

    @Test
    void ipAndUserMode_ipBlocked_throws() throws Throwable {
        UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        setAuthenticatedUser(userId);

        when(invocation.getMethod()).thenReturn(method("ipAndUser"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(store.tryConsume(contains(":IP:1.2.3.4"), anyInt(), anyInt())).thenReturn(BLOCKED);

        assertThatThrownBy(() -> interceptor.invoke(invocation))
            .isInstanceOf(RateLimitExceededException.class);
    }

    // ── Multiple @RateLimiting annotations ───────────────────────────────

    @Test
    void multipleAnnotations_allPass_worstCaseHeaders() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("multipleRules"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        // First rule: 9 remaining; second rule (USER, no principal): skipped
        when(store.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(ALLOWED);
        when(invocation.proceed()).thenReturn(null);

        interceptor.invoke(invocation);

        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    // ── Key includes endpoint ────────────────────────────────────────────

    @Test
    void keyContainsEndpointClassName() throws Throwable {
        when(invocation.getMethod()).thenReturn(method("ipOnly"));
        when(ipResolver.resolve(any())).thenReturn("1.2.3.4");
        when(store.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(ALLOWED);
        when(invocation.proceed()).thenReturn(null);

        interceptor.invoke(invocation);

        verify(store).tryConsume(contains("TestEndpoints.ipOnly"), anyInt(), anyInt());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void setAuthenticatedUser(UUID userId) {
        CustomUserDetails details = mock(CustomUserDetails.class);
        when(details.getUserId()).thenReturn(userId);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private static Method method(String name) throws NoSuchMethodException {
        return TestEndpoints.class.getMethod(name);
    }

    @SuppressWarnings("unused")
    static class TestEndpoints {
        @RateLimiting(mode = RateLimitMode.IP, max = 10, windowSeconds = 60)
        public void ipOnly() {}

        @RateLimiting(mode = RateLimitMode.USER, max = 5, windowSeconds = 300)
        public void userOnly() {}

        @RateLimiting(mode = RateLimitMode.IP_AND_USER, max = 10, windowSeconds = 60)
        public void ipAndUser() {}

        @RateLimiting(mode = RateLimitMode.IP, max = 10, windowSeconds = 60)
        @RateLimiting(mode = RateLimitMode.USER, max = 5, windowSeconds = 300)
        public void multipleRules() {}
    }
}