package com.synapsys.api.agent.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExceptionTest {

    @Test
    void allSubtypesAreAgentExceptionsWithMessages() {
        AgentException[] all = {
            new AgentException.TokenNotFound(),
            new AgentException.TokenNotConsumable(),
            new AgentException.TokenNotRevocable(),
            new AgentException.InvalidPublicKey(),
            new AgentException.PublicKeyAlreadyRegistered(),
            new AgentException.AgentNotFound(),
            new AgentException.AgentNotRevocable(),
            new AgentException.AgentNotDeletable(),
            new AgentException.HandshakeFailed(),
            new AgentException.DataIntegrityError()
        };
        for (AgentException e : all) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isNotBlank();
        }
    }
}
