package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.EnrollmentTokenView;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

public interface ListEnrollmentTokensUseCase {
    PageResult<EnrollmentTokenView> list(int page, int size, SortRequest sort);
}
