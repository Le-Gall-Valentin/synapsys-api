package com.synapsys.api.agent.infrastructure.security;

import com.synapsys.api.identity.application.port.in.FindUserUseCase;
import com.synapsys.api.identity.domain.model.User;
import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryAdapterTest {

    @Mock FindUserUseCase findUser;

    @Test
    void usernamesByIds_mapsIdToUsername() {
        UUID a = UUID.randomUUID();
        when(findUser.findByIds(Set.of(a))).thenReturn(List.of(
            new User(a, "alice", "alice@example.com", Role.USER, true, Instant.now())));

        Map<UUID, String> result = new UserDirectoryAdapter(findUser).usernamesByIds(Set.of(a));

        assertThat(result).containsEntry(a, "alice");
    }

    @Test
    void usernamesByIds_emptyInput_returnsEmptyMapWithoutCallingIdentity() {
        Map<UUID, String> result = new UserDirectoryAdapter(findUser).usernamesByIds(Set.of());
        assertThat(result).isEmpty();
    }

    @Test
    void usernamesByIds_nullUsername_isOmittedWithoutFailing() {
        UUID a = UUID.randomUUID();
        when(findUser.findByIds(Set.of(a))).thenReturn(List.of(
            new User(a, null, "ghost@example.com", Role.USER, true, Instant.now())));

        Map<UUID, String> result = new UserDirectoryAdapter(findUser).usernamesByIds(Set.of(a));

        assertThat(result).doesNotContainKey(a);
    }
}
