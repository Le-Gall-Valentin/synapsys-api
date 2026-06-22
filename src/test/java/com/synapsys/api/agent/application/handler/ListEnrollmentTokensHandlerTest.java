package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.EnrollmentTokenStatus;
import com.synapsys.api.agent.domain.model.EnrollmentTokenView;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEnrollmentTokensHandlerTest {

    @Mock EnrollmentTokenRepository repository;

    @Test
    void list_derivesStatusForEachToken() {
        Instant now = Instant.now();
        UUID creator = UUID.randomUUID();
        EnrollmentToken active = new EnrollmentToken(UUID.randomUUID(), "a", null, null, null,
            now.plus(1, ChronoUnit.HOURS), now, creator);
        EnrollmentToken expired = new EnrollmentToken(UUID.randomUUID(), "b", null, null, null,
            now.minus(1, ChronoUnit.HOURS), now, creator);
        when(repository.findAll(0, 20, SortRequest.descBy("createdAt")))
            .thenReturn(new PageResult<>(List.of(active, expired), 2, 0, 20));

        var handler = new ListEnrollmentTokensHandler(repository);
        PageResult<EnrollmentTokenView> result = handler.list(0, 20, SortRequest.descBy("createdAt"));

        assertThat(result.content()).extracting(EnrollmentTokenView::status)
            .containsExactly(EnrollmentTokenStatus.ACTIVE, EnrollmentTokenStatus.EXPIRED);
    }
}
