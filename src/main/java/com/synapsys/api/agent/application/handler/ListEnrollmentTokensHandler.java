package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.ListEnrollmentTokensUseCase;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.EnrollmentTokenView;
import com.synapsys.api.agent.domain.model.TokenCreator;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.agent.domain.port.out.UserDirectoryPort;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class ListEnrollmentTokensHandler implements ListEnrollmentTokensUseCase {

    private final EnrollmentTokenRepository repository;
    private final UserDirectoryPort userDirectory;

    public ListEnrollmentTokensHandler(EnrollmentTokenRepository repository, UserDirectoryPort userDirectory) {
        this.repository = repository;
        this.userDirectory = userDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<EnrollmentTokenView> list(int page, int size, SortRequest sort) {
        Instant now = Instant.now();
        PageResult<EnrollmentToken> result = repository.findAll(page, size, sort);
        Set<UUID> creatorIds = result.content().stream()
            .map(EnrollmentToken::createdBy)
            .collect(Collectors.toSet());
        var usernames = userDirectory.usernamesByIds(creatorIds);
        List<EnrollmentTokenView> views = result.content().stream()
            .map(t -> new EnrollmentTokenView(
                t.id(), t.serverName(), t.deriveStatus(now), t.expiresAt(),
                new TokenCreator(t.createdBy(), usernames.get(t.createdBy())), t.createdAt()))
            .toList();
        return new PageResult<>(views, result.totalElements(), result.page(), result.size());
    }
}
