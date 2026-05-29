package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.VerifyTotpChallengeUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// @Transactional is required here because RefreshTokenGenerator (RefreshTokenIssuerPort)
// uses Propagation.MANDATORY and must run inside an existing transaction.
// Redis operations (challenge store) are not part of this transaction and not rolled back
// on JPA failure — this is an accepted trade-off documented in the architecture decisions.
@ApplicationService
public class VerifyTotpChallengeHandler implements VerifyTotpChallengeUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final TotpChallengeStorePort challengeStore;
    private final TotpCodeValidatorPort codeValidator;
    private final UserRepository userRepository;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenIssuerPort refreshTokenPort;
    private final int refreshTokenExpiryDays;

    public VerifyTotpChallengeHandler(TotpChallengeStorePort challengeStore,
                                      TotpCodeValidatorPort codeValidator,
                                      UserRepository userRepository,
                                      AccessTokenPort accessTokenPort,
                                      RefreshTokenIssuerPort refreshTokenPort,
                                      RefreshTokenConfigPort tokenConfig) {
        this.challengeStore = challengeStore;
        this.codeValidator = codeValidator;
        this.userRepository = userRepository;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
        this.refreshTokenExpiryDays = tokenConfig.refreshTokenExpiryDays();
    }

    @Override
    @Transactional
    public LoginResult.Success verify(VerifyTotpChallengeCommand command) {
        UUID userId = challengeStore.resolveChallenge(command.challengeId())
            .orElseThrow(AuthException.TotpChallengeExpired::new);

        User user = userRepository.findById(userId)
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.isActive()) {
            throw new AuthException.UserNotActive();
        }

        // Validate cryptographically first — wrong code must NOT consume the anti-replay slot,
        // so the user can retry with the next TOTP window's code.
        if (!codeValidator.isValid(user.totpSecret(), command.code())) {
            int attempts = challengeStore.incrementFailedAttempts(command.challengeId());
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                challengeStore.invalidateChallenge(command.challengeId());
                throw new AuthException.TotpChallengeExpired();
            }
            throw new AuthException.TotpCodeInvalid();
        }

        // Atomic SETNX: consume the code, reject if already consumed (concurrent replay).
        if (!challengeStore.markCodeUsedIfAbsent(userId, command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        challengeStore.invalidateChallenge(command.challengeId());

        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(user),
            refreshTokenPort.generate(user, refreshTokenExpiryDays)
        );
        return new LoginResult.Success(tokens, user);
    }
}