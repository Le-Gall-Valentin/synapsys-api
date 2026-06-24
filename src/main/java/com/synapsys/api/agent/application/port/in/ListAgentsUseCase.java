package com.synapsys.api.agent.application.port.in;

import com.synapsys.api.agent.domain.model.AgentView;
import com.synapsys.api.shared.model.PageResult;
import com.synapsys.api.shared.model.SortRequest;

public interface ListAgentsUseCase {
    PageResult<AgentView> list(int page, int size, SortRequest sort, String search);
}
