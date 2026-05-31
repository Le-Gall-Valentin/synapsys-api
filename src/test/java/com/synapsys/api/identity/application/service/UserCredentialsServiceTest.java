package com.synapsys.api.identity.application.service;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialsServiceTest {

    @Mock UserRepository userRepository;

    private UserCredentialsService service;

    @BeforeEach
    void setUp() {
        service = new UserCredentialsService(userRepository);
    }

    @Test
    void findByUsername_found_returnsUserInfo() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = new User(id, "alice", "alice@test.com", Role.USER, true, now);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<UserCredentialsService.UserInfo> result = service.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().username()).isEqualTo("alice");
        assertThat(result.get().email()).isEqualTo("alice@test.com");
        assertThat(result.get().isActive()).isTrue();
        assertThat(result.get().role()).isEqualTo(Role.USER);
        assertThat(result.get().createdAt()).isEqualTo(now);
    }

    @Test
    void findByUsername_notFound_returnsEmpty() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<UserCredentialsService.UserInfo> result = service.findByUsername("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findById_found_returnsUserInfo() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "bob", "bob@test.com", Role.ADMIN, true, Instant.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Optional<UserCredentialsService.UserInfo> result = service.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().username()).isEqualTo("bob");
        assertThat(result.get().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Optional<UserCredentialsService.UserInfo> result = service.findById(id);

        assertThat(result).isEmpty();
    }
}