package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.ListEnrollmentTokensUseCase;
import com.synapsys.api.agent.domain.model.EnrollmentToken;
import com.synapsys.api.agent.domain.model.EnrollmentTokenView;
import com.synapsys.api.agent.domain.port.out.EnrollmentTokenRepository;
import com.synapsys.api.shared.annotation.ApplicationService;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

import java.time.Instant;
import java.util.List;

@ApplicationService
public class ListEnrollmentTokensHandler implements ListEnrollmentTokensUseCase {

    private final EnrollmentTokenRepository repository;

    public ListEnrollmentTokensHandler(EnrollmentTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<EnrollmentTokenView> list(int page, int size, SortRequest sort) {
        Instant now = Instant.now();
        PageResult<EnrollmentToken> result = repository.findAll(page, size, sort);
        List<EnrollmentTokenView> views = result.content().stream()
            .map(t -> new EnrollmentTokenView(
                t.id(), t.serverName(), t.deriveStatus(now), t.expiresAt(), t.createdBy(), t.createdAt()))
            .toList();
        return new PageResult<>(views, result.totalElements(), result.page(), result.size());
    }
}
