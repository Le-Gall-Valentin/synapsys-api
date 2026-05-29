package com.synapsys.api.auth.domain.port.out;

public interface TotpCodeValidatorPort {
    boolean isValid(String secret, String code);
}