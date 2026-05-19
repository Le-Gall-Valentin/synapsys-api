package com.synapsys.api.auth.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AtomicLong clock = new AtomicLong(0);
    private LoginRateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter(clock::get, List.of(), MAPPER);
        chain = mock(FilterChain.class);
    }

    @Test
    void nonLoginPostPassesThrough() throws Exception {
        MockHttpServletRequest req = postTo("/api/other");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void getRequestToLoginPassesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", LOGIN_PATH);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    void tenAttemptsFromSameIpAreAllowed() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("1.2.3.4"), res, chain);
            assertThat(res.getStatus()).as("attempt %d", i + 1).isEqualTo(200);
        }
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void eleventhAttemptIsRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("5.5.5.5"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequest("5.5.5.5"), res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void attemptsOutsideWindowAreNotCounted() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("7.7.7.7"), new MockHttpServletResponse(), chain);
        }

        clock.set(61_000L);

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequest("7.7.7.7"), res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain, times(11)).doFilter(any(), any());
    }

    @Test
    void rateLimitIsPerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("10.0.0.1"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.2"), res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void refreshPathIsRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilter(refreshRequest("3.3.3.3"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(refreshRequest("3.3.3.3"), res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void usesXForwardedForFromTrustedProxy() throws Exception {
        LoginRateLimitFilter filterWithProxy = new LoginRateLimitFilter(clock::get, List.of("10.0.0.1"), MAPPER);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = loginRequest("10.0.0.1");
            req.addHeader("X-Forwarded-For", "1.2.3.4");
            filterWithProxy.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest req = loginRequest("10.0.0.1");
        req.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filterWithProxy.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void doesNotUseXForwardedForFromUntrustedProxy() throws Exception {
        LoginRateLimitFilter filterWithProxy = new LoginRateLimitFilter(clock::get, List.of("10.0.0.1"), MAPPER);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = loginRequest("9.9.9.9");
            req.addHeader("X-Forwarded-For", "1.2.3.4");
            filterWithProxy.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest req2 = loginRequest("9.9.9.9");
        req2.addHeader("X-Forwarded-For", "5.6.7.8");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filterWithProxy.doFilter(req2, res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
    }

    // ── Username rate limit tests ──────────────────────────────────────────

    @Test
    void twentyAttemptsForSameUsernameAreAllowed() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequestWithBody("2.2.2." + i, "victim"), res, chain);
            assertThat(res.getStatus()).as("attempt %d", i + 1).isEqualTo(200);
        }
    }

    @Test
    void twentyFirstAttemptForSameUsernameIsRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            filter.doFilter(loginRequestWithBody("3.3." + i + ".1", "target"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequestWithBody("3.3.21.1", "target"), res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void usernameLimitIsCaseInsensitive() throws Exception {
        for (int i = 0; i < 20; i++) {
            filter.doFilter(loginRequestWithBody("4.4." + i + ".1", "Alice"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequestWithBody("4.4.21.1", "ALICE"), res, chain);

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void requestWithoutUsernameBodyOnlyUsesIpLimit() throws Exception {
        // 5 attempts with no body — only IP counter increments
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("6.6.6.6"), new MockHttpServletResponse(), chain);
        }
        // 5 more with a username — username counter starts, IP counter keeps going
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequestWithBody("6.6.6.6", "user"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequestWithBody("6.6.6.6", "user"), res, chain);

        // IP limit (10) reached — should be 429
        assertThat(res.getStatus()).isEqualTo(429);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest req = postTo(LOGIN_PATH);
        req.setRemoteAddr(remoteAddr);
        return req;
    }

    private static MockHttpServletRequest loginRequestWithBody(String remoteAddr, String username) {
        MockHttpServletRequest req = loginRequest(remoteAddr);
        req.setContentType("application/json");
        req.setContent(("{\"username\":\"" + username + "\",\"password\":\"pass\"}").getBytes(StandardCharsets.UTF_8));
        return req;
    }

    private static MockHttpServletRequest refreshRequest(String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
        req.setRemoteAddr(remoteAddr);
        return req;
    }

    private static MockHttpServletRequest postTo(String path) {
        return new MockHttpServletRequest("POST", path);
    }
}