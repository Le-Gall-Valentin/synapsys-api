package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserHandlerTest {

    @Mock UserRepository userRepository;

    private GetCurrentUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetCurrentUserHandler(userRepository);
    }

    @Test
    void getCurrentUser_found_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "user1", "u@test.com", "hash", Role.USER, true, Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = handler.getCurrentUser(id);
        assertThat(result.id()).isEqualTo(id);
    }

    @Test
    void getCurrentUser_notFound_throwsUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.getCurrentUser(id))
            .isInstanceOf(AuthException.UserNotFound.class);
    }
}