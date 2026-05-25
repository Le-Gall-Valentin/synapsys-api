package com.synapsys.api.auth.infrastructure.web;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.auth.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private AuthExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new AuthExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
    }

    @Test
    void handle_invalidCredentials_returns401() {
        var response = handler.handle(new AuthException.InvalidCredentials(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handle_tokenExpired_returns401() {
        var response = handler.handle(new AuthException.TokenExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handle_tokenRevoked_returns401() {
        var response = handler.handle(new AuthException.TokenRevoked(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handle_userNotActive_returns403WithGenericUserNotActiveMessage() {
        var response = handler.handle(new AuthException.UserNotActive(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("User account is not active.");
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotActive");
    }

    @Test
    void handle_insufficientPermissions_returns403() {
        var response = handler.handle(
            new AuthException.InsufficientPermissions(Role.USER, Role.ADMIN), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handle_userNotFound_returns404() {
        var response = handler.handle(new AuthException.UserNotFound(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("User not found");
        assertThat(response.getBody().getTitle()).isEqualTo("UserNotFound");
    }

    @Test
    void handle_usernameAlreadyExists_returns409() {
        var response = handler.handle(new AuthException.UsernameAlreadyExists(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handle_dataIntegrityError_returns500WithGenericMessage() {
        var response = handler.handle(new AuthException.DataIntegrityError("uq_users_email"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        // Constraint name must NOT be exposed to the client
        assertThat(response.getBody().getDetail()).doesNotContain("uq_users_email");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void handle_setsInstanceUri() {
        var response = handler.handle(new AuthException.InvalidCredentials(), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInstance()).isNotNull();
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
    }

    @Test
    void handle_insufficientPermissions_doesNotLeakRoleNames() {
        var response = handler.handle(
            new AuthException.InsufficientPermissions(Role.USER, Role.ADMIN), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Insufficient permissions");
        assertThat(response.getBody().getDetail()).doesNotContain("USER");
        assertThat(response.getBody().getDetail()).doesNotContain("ADMIN");
    }

    @Test
    void handle_tokenRevoked_returnsGenericAuthenticationRequired() {
        var response = handler.handle(new AuthException.TokenRevoked(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }

    @Test
    void handle_tokenExpired_returnsGenericAuthenticationRequired() {
        var response = handler.handle(new AuthException.TokenExpired(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Authentication required");
    }
}