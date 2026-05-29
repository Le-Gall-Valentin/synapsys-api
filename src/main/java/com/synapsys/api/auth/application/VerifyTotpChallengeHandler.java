package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.VerifyTotpChallengeUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;

import java.util.UUID;

// Note: @Transactional omitted intentionally — there are no JPA mutations here.
// Redis and JPA operations are not atomically coordinated; challenge invalidation
// and code marking happen in Redis only, while token generation is stateless.
@ApplicationService
public class VerifyTotpChallengeHandler implements VerifyTotpChallengeUseCase {

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