package com.synapsys.api.auth.infrastructure.config;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class TotpEncryptionConfig {

    // Fixed application-level salt (hex, 16 bytes). Security comes from the master secret
    // (PBKDF2 key stretching), not from this salt. Changing it would invalidate all stored secrets.
    private static final String SALT = "73796e617073797300112233445566aa";

    // Encryptors.delux uses AES-256 in GCM mode with a random IV per encryption call.
    // Bean name "totpSecretEncryptor" — qualify injection sites if multiple TextEncryptors exist.
    @Bean("totpSecretEncryptor")
    TextEncryptor totpSecretEncryptor(SynapsysProperties properties) {
        return Encryptors.delux(properties.encryption().secret(), SALT);
    }
}