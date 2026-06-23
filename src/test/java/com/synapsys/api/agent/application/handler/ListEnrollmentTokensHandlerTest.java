package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.EnrollmentTokenStatus;
import com.synapsys.api.agent.domain.model.EnrollmentTokenView;
import com.synapsys.api.agent.domain.model.TokenCreator;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.agent.domain.port.out.UserDirectoryPort;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEnrollmentTokensHandlerTest {

    @Mock EnrollmentTokenRepository repository;
    @Mock UserDirectoryPort userDirectory;
    @Captor ArgumentCaptor<Collection<UUID>> idsCaptor;

    @Test
    void list_derivesStatus_resolvesUsernames_andDedupesCreators() {
        Instant now = Instant.now();
        UUID creator = UUID.randomUUID();
        EnrollmentToken active = new EnrollmentToken(UUID.randomUUID(), "a", null, null, null,
            now.plus(1, ChronoUnit.HOURS), now, creator);
        EnrollmentToken expired = new EnrollmentToken(UUID.randomUUID(), "b", null, null, null,
            now.minus(1, ChronoUnit.HOURS), now, creator);
        when(repository.findAll(0, 20, SortRequest.descBy("createdAt")))
            .thenReturn(new PageResult<>(List.of(active, expired), 2, 0, 20));
        when(userDirectory.usernamesByIds(idsCaptor.capture()))
            .thenReturn(Map.of(creator, "superadmin"));

        var handler = new ListEnrollmentTokensHandler(repository, userDirectory);
        PageResult<EnrollmentTokenView> result = handler.list(0, 20, SortRequest.descBy("createdAt"));

        assertThat(result.content()).extracting(EnrollmentTokenView::status)
            .containsExactly(EnrollmentTokenStatus.ACTIVE, EnrollmentTokenStatus.EXPIRED);
        assertThat(result.content()).extracting(v -> v.createdBy().username())
            .containsExactly("superadmin", "superadmin");
        // both tokens share one creator → a single deduped id is queried
        assertThat(idsCaptor.getValue()).containsExactly(creator);
    }

    @Test
    void list_unknownCreator_yieldsNullUsername() {
        Instant now = Instant.now();
        UUID creator = UUID.randomUUID();
        EnrollmentToken token = new EnrollmentToken(UUID.randomUUID(), "a", null, null, null,
            now.plus(1, ChronoUnit.HOURS), now, creator);
        when(repository.findAll(0, 20, SortRequest.descBy("createdAt")))
            .thenReturn(new PageResult<>(List.of(token), 1, 0, 20));
        when(userDirectory.usernamesByIds(Set.of(creator))).thenReturn(Map.of());

        var handler = new ListEnrollmentTokensHandler(repository, userDirectory);
        PageResult<EnrollmentTokenView> result = handler.list(0, 20, SortRequest.descBy("createdAt"));

        assertThat(result.content().get(0).createdBy().id()).isEqualTo(creator);
        assertThat(result.content().get(0).createdBy().username()).isNull();
    }
}
