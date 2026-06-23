package com.synapsys.api.agent.application.handler;

import com.synapsys.api.agent.application.port.in.OpenAgentChallengeUseCase;
import com.synapsys.api.agent.domain.port.out.AgentChallengeStorePort;
import com.synapsys.api.shared.annotation.ApplicationService;

@ApplicationService
public class OpenAgentChallengeHandler implements OpenAgentChallengeUseCase {

    private final AgentChallengeStorePort challengeStore;

    public OpenAgentChallengeHandler(AgentChallengeStorePort challengeStore) {
        this.challengeStore = challengeStore;
    }

    @Override
    public String openChallenge(String connectionId) {
        return challengeStore.issueChallenge(connectionId);
    }
}
