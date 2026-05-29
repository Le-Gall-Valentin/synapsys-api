package com.synapsys.api.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SynapsysPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void jwtProperties_rejectsZeroExpiryMinutes() {
        var props = new SynapsysProperties.JwtProperties("valid-secret", 0, "synapsys-api", "synapsys-api");
        var violations = validator.validate(props);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void jwtProperties_rejectsNegativeExpiryMinutes() {
        var props = new SynapsysProperties.JwtProperties("valid-secret", -5, "synapsys-api", "synapsys-api");
        var violations = validator.validate(props);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void jwtProperties_acceptsPositiveExpiryMinutes() {
        var props = new SynapsysProperties.JwtProperties("valid-secret", 15, "synapsys-api", "synapsys-api");
        var violations = validator.validate(props);
        assertThat(violations).isEmpty();
    }
}