package com.synapsys.api.authentication.infrastructure.web;

import com.synapsys.api.authentication.domain.model.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationExceptionHandlerTest {

    private AuthenticationExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new AuthenticationExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
    }

    @Test
    void handle_invalidCredentials_returns401() {
        var response = handler.handle(new AuthenticationException.InvalidCredentials(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handle_tokenExpired_returns401() {
        var response = handler.handle(new AuthenticationException.TokenExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handle_tokenNotFound_returns401() {
        var response = handler.handle(new AuthenticationException.TokenNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handle_tokenRevoked_returns401() {
        var response = handler.handle(new AuthenticationException.TokenRevoked(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handle_userNotActive_returns403WithGenericUserNotActiveMessage() {
        var response = handler.handle(new AuthenticationException.UserNotActive(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("User account is not active.");
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotActive");
    }

    @Test
    void handle_userNotFound_returns404() {
        var response = handler.handle(new AuthenticationException.UserNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("User not found");
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotFound");
    }

    @Test
    void handle_setsInstanceUri() {
        var response = handler.handle(new AuthenticationException.InvalidCredentials(), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInstance()).isNotNull();
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
    }

    @Test
    void handle_tokenRevoked_returnsGenericAuthenticationRequired() {
        var response = handler.handle(new AuthenticationException.TokenRevoked(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }

    @Test
    void handle_tokenExpired_returnsGenericAuthenticationRequired() {
        var response = handler.handle(new AuthenticationException.TokenExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }
}