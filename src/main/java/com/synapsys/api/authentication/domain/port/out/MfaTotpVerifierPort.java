package com.synapsys.api.authentication.domain.port.out;

import java.util.UUID;

/**
 * Port inter-BC : authentication définit, mfa implémente via authentication.infrastructure.
 * Valide le code TOTP et le consomme atomiquement (anti-replay).
 * Retourne false si invalide, expiré, ou déjà utilisé. Ne propage jamais d'exception mfa.
 */
public interface MfaTotpVerifierPort {
    boolean verifyAndConsume(UUID userId, String code);
}