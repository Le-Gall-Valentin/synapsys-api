package com.synapsys.api.agent.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNonPositiveValidityHours() {
        var props = new AgentProperties(0, 30, 90, "synenr_", "/ws/agents");
        assertThat(validator.validate(props)).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveChallengeTtl() {
        var props = new AgentProperties(24, 0, 90, "synenr_", "/ws/agents");
        assertThat(validator.validate(props)).isNotEmpty();
    }

    @Test
    void rejectsNonPositivePresenceTtl() {
        var props = new AgentProperties(24, 30, -1, "synenr_", "/ws/agents");
        assertThat(validator.validate(props)).isNotEmpty();
    }

    @Test
    void acceptsValidValues() {
        var props = new AgentProperties(24, 30, 90, "synenr_", "/ws/agents");
        assertThat(validator.validate(props)).isEmpty();
    }
}