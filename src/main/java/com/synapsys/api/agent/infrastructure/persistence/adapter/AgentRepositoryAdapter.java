package com.synapsys.api.agent.infrastructure.persistence.adapter;

import com.synapsys.api.agent.domain.model.Agent;
import com.synapsys.api.agent.domain.model.AgentException;
import com.synapsys.api.agent.domain.model.AgentLifecycleStatus;
import com.synapsys.api.agent.domain.model.NewAgent;
import com.synapsys.api.agent.domain.port.out.AgentRepository;
import com.synapsys.api.agent.infrastructure.persistence.entity.AgentEntity;
import com.synapsys.api.agent.infrastructure.persistence.repository.AgentJpaRepository;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class AgentRepositoryAdapter implements AgentRepository {

    private final AgentJpaRepository jpa;

    public AgentRepositoryAdapter(AgentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Agent insert(NewAgent newAgent) {
        try {
            AgentEntity e = new AgentEntity(newAgent.serverName(), encode(newAgent.publicKey()),
                newAgent.fingerprint(), newAgent.enrollmentTokenId(), newAgent.enrolledBy());
            return toDomain(jpa.saveAndFlush(e));
        } catch (DataIntegrityViolationException ex) {
            throw resolve(ex);
        }
    }

    @Override
    public Optional<Agent> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByPublicKey(byte[] publicKey) {
        return jpa.existsByPublicKey(encode(publicKey));
    }

    @Override
    public boolean markConnected(UUID id, Instant when, String ip) {
        return jpa.markConnected(id, when, ip) == 1;
    }

    @Override
    public void updateActivitySnapshot(UUID id, Instant lastActivityAt, String ip) {
        jpa.updateActivitySnapshot(id, lastActivityAt, ip);
    }

    @Override
    public boolean markRevoked(UUID id, UUID revokedBy, Instant now) {
        return jpa.markRevoked(id, revokedBy, now, AgentLifecycleStatus.REVOKED) == 1;
    }

    @Override
    public boolean delete(UUID id) {
        return jpa.deleteIfRevoked(id) == 1;
    }

    @Override
    public PageResult<Agent> findAll(int page, int size, SortRequest sort, String search) {
        Sort springSort = sort.ascending()
            ? Sort.by(sort.field()).ascending()
            : Sort.by(sort.field()).descending();
        PageRequest pageRequest = PageRequest.of(page, size, springSort);
        Page<AgentEntity> result = (search == null || search.isBlank())
            ? jpa.findAll(pageRequest)
            : jpa.searchAll(toLikePattern(search), pageRequest);
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(),
            result.getTotalElements(), page, size);
    }

    /** Échappe les wildcards LIKE avec '!' (cohérent avec la clause ESCAPE de la requête). */
    private static String toLikePattern(String search) {
        String escaped = search.trim().toLowerCase(Locale.ROOT)
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
        return "%" + escaped + "%";
    }

    @Override
    public List<Agent> findAllNonRevoked() {
        return jpa.findAllNonRevoked().stream().map(this::toDomain).toList();
    }

    @Override
    public long countRevoked() {
        return jpa.countRevoked();
    }

    private static String encode(byte[] publicKey) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey);
    }

    private static byte[] decode(String publicKey) {
        return Base64.getUrlDecoder().decode(publicKey);
    }

    private Agent toDomain(AgentEntity e) {
        return new Agent(e.getId(), e.getServerName(), decode(e.getPublicKey()), e.getFingerprint(),
            e.getStatus(), e.getEnrollmentTokenId(), e.getEnrolledAt(), e.getEnrolledBy(),
            e.getFirstConnectedAt(), e.getLastActivityAt(), e.getIpAddress(), e.getRevokedAt(), e.getRevokedBy());
    }

    private AgentException resolve(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String c = cve.getConstraintName();
            if (c != null) {
                if (c.contains("uq_agents_public_key") || c.contains("uq_agents_fingerprint")) {
                    return new AgentException.PublicKeyAlreadyRegistered();
                }
                if (c.contains("uq_agents_server_name_active")) {
                    return new AgentException.ServerNameInUse();
                }
            }
        }
        return new AgentException.DataIntegrityError();
    }
}
