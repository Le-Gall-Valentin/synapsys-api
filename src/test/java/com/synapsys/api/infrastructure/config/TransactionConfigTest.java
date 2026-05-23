package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.application.RefreshTokenHandler;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TransactionConfigTest {

    @Test
    void refreshTokenHandler_hasRefreshMethod() {
        assertThatNoException().isThrownBy(() ->
            RefreshTokenHandler.class.getMethod("refresh", String.class)
        );
    }

    @Test
    void refreshTokenHandler_refreshMethod_hasTransactionalWithNoRollbackForTokenRevoked() throws Exception {
        Method method = RefreshTokenHandler.class.getMethod("refresh", String.class);
        Transactional tx = method.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.noRollbackFor()).contains(
            com.synapsys.api.auth.domain.model.AuthException.TokenRevoked.class
        );
    }
}