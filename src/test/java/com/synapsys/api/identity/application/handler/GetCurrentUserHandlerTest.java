package com.synapsys.api.identity.application.handler;

import com.synapsys.api.identity.domain.model.IdentityException;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.identity.domain.port.out.UserRepository;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
        User user = new User(id, "user1", "u@test.com", Role.USER, true, Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = handler.getCurrentUser(id);
        assertThat(result).isEqualTo(user);
    }

    @Test
    void getCurrentUser_notFound_throwsUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.getCurrentUser(id))
            .isInstanceOf(IdentityException.UserNotFound.class);
    }

    @Test
    void getCurrentUser_inactiveUser_throwsUserNotActive() {
        UUID id = UUID.randomUUID();
        User inactive = new User(id, "user1", "u@test.com", Role.USER, false, Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> handler.getCurrentUser(id))
            .isInstanceOf(IdentityException.UserNotActive.class);
    }
}