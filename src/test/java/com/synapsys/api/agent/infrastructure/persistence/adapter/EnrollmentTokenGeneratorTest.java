package com.synapsys.api.agent.infrastructure.persistence.adapter;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentTokenGeneratorTest {

    @Mock EnrollmentTokenRepository repository;
    @Mock AgentTokenHashPort hashPort;

    private EnrollmentTokenGenerator generator;
    private final UUID creator = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var props = new AgentProperties(24, 30, 90, "synenr_", "/ws/agents");
        generator = new EnrollmentTokenGenerator(repository, hashPort, props);
    }

    @Test
    void issue_generatesPrefixedHighEntropyToken_hashesIt_andPersists() {
        when(hashPort.hash(any())).thenReturn("the-hash");
        ArgumentCaptor<NewEnrollmentToken> captor = ArgumentCaptor.forClass(NewEnrollmentToken.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> {
            NewEnrollmentToken n = inv.getArgument(0);
            return new EnrollmentToken(UUID.randomUUID(), n.serverName(), null, null, null,
                n.expiresAt(), Instant.now(), n.createdBy());
        });

        Instant before = Instant.now();
        IssuedToken issued = generator.issue("web-01", creator);

        assertThat(issued.rawToken()).startsWith("synenr_");
        assertThat(issued.rawToken().length()).isGreaterThan("synenr_".length() + 40); // 32 bytes base64url ~ 43 chars
        assertThat(captor.getValue().tokenHash()).isEqualTo("the-hash");
        assertThat(captor.getValue().serverName()).isEqualTo("web-01");
        assertThat(captor.getValue().createdBy()).isEqualTo(creator);
        assertThat(issued.expiresAt()).isAfter(before.plus(23, ChronoUnit.HOURS));
        assertThat(issued.expiresAt()).isBefore(before.plus(25, ChronoUnit.HOURS));
    }
}
