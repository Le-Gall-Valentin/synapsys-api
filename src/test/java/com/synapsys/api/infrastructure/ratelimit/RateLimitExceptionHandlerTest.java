package com.synapsys.api.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceptionHandlerTest {

    private RateLimitExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new RateLimitExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
    }

    @Test
    void handle_rateLimitExceeded_returns429WithProblemDetail() {
        var response = handler.handle(new RateLimitExceededException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Too many requests. Please try again later.");
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }
}