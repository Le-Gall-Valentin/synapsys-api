package com.synapsys.api.agent.infrastructure.persistence.adapter;

import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.IssuedToken;
import com.synapsys.api.agent.domain.model.NewEnrollmentToken;
import com.synapsys.api.agent.domain.port.out.AgentTokenHashPort;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.agent.infrastructure.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentTokenGeneratorTest {

    @Mock EnrollmentTokenRepository repository;
    @Mock AgentTokenHashPort hashPort;

    private EnrollmentTokenGenerator generator;
    private final UUID creator = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var props = new AgentProperties(24, 30, 90, 10, "synenr_", "/ws/agents");
        generator = new EnrollmentTokenGenerator(repository, hashPort, props);
        lenient().when(hashPort.hash(any())).thenReturn("the-hash");
        lenient().when(repository.save(any())).thenAnswer(inv -> {
            NewEnrollmentToken n = inv.getArgument(0);
            return new EnrollmentToken(UUID.randomUUID(), n.serverName(), null, null, null,
                n.expiresAt(), Instant.now(), n.createdBy());
        });
    }

    @Test
    void issue_nullTtl_usesConfiguredMaximum() {
        Instant before = Instant.now();
        IssuedToken issued = generator.issue("web-01", null, creator);
        assertThat(issued.rawToken()).startsWith("synenr_");
        assertThat(issued.expiresAt()).isAfter(before.plus(23, ChronoUnit.HOURS));
        assertThat(issued.expiresAt()).isBefore(before.plus(25, ChronoUnit.HOURS));
    }

    @Test
    void issue_persistsHashOfRawToken_serverName_andCreator() {
        ArgumentCaptor<NewEnrollmentToken> captor = ArgumentCaptor.forClass(NewEnrollmentToken.class);

        IssuedToken issued = generator.issue("web-01", null, creator);

        verify(repository).save(captor.capture());
        NewEnrollmentToken persisted = captor.getValue();
        // the raw token (not the server name) is what gets hashed and stored
        verify(hashPort).hash(issued.rawToken());
        assertThat(persisted.tokenHash()).isEqualTo("the-hash");
        assertThat(persisted.serverName()).isEqualTo("web-01");
        assertThat(persisted.createdBy()).isEqualTo(creator);
        // 32 random bytes, base64url without padding ~ 43 chars after the prefix
        assertThat(issued.rawToken().length()).isGreaterThan("synenr_".length() + 40);
    }

    @Test
    void issue_shorterTtl_isHonored() {
        Instant before = Instant.now();
        IssuedToken issued = generator.issue("web-01", Duration.ofMinutes(15), creator);
        assertThat(issued.expiresAt()).isAfter(before.plus(14, ChronoUnit.MINUTES));
        assertThat(issued.expiresAt()).isBefore(before.plus(16, ChronoUnit.MINUTES));
    }

    @Test
    void issue_ttlAboveMaximum_throwsInvalidTokenTtl() {
        assertThatThrownBy(() -> generator.issue("web-01", Duration.ofHours(25), creator))
            .isInstanceOf(AgentException.InvalidTokenTtl.class);
    }

    @Test
    void issue_zeroOrNegativeTtl_throwsInvalidTokenTtl() {
        assertThatThrownBy(() -> generator.issue("web-01", java.time.Duration.ZERO, creator))
            .isInstanceOf(AgentException.InvalidTokenTtl.class);
        assertThatThrownBy(() -> generator.issue("web-01", java.time.Duration.ofMinutes(-5), creator))
            .isInstanceOf(AgentException.InvalidTokenTtl.class);
    }
}
