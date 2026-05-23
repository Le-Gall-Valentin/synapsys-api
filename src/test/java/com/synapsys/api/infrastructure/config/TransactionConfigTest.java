package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.application.RefreshTokenHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class TransactionConfigTest {

    @Test
    void refreshTokenHandler_hasRefreshMethod_matchingTransactionConfig() {
        // TransactionConfig binds a NoRollback rule to the method named "refresh".
        // This test fails if RefreshTokenHandler.refresh() is renamed, catching the mismatch early.
        assertThatNoException().isThrownBy(() ->
            RefreshTokenHandler.class.getMethod("refresh", String.class)
        );
    }
}