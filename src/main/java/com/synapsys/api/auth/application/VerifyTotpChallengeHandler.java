package com.synapsys.api.auth.application;

import com.synapsys.api.auth.domain.model.*;
import com.synapsys.api.auth.domain.port.in.VerifyTotpChallengeUseCase;
import com.synapsys.api.auth.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    @Transactional
    public LoginResult.Success verify(VerifyTotpChallengeCommand command) {
        UUID userId = challengeStore.resolveChallenge(command.challengeId())
            .orElseThrow(AuthException.TotpChallengeExpired::new);

        User user = userRepository.findById(userId)
            .orElseThrow(AuthException.UserNotFound::new);

        if (!user.isActive()) {
            throw new AuthException.UserNotActive();
        }

        if (challengeStore.isCodeAlreadyUsed(userId, command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        if (!codeValidator.isValid(user.totpSecret(), command.code())) {
            throw new AuthException.TotpCodeInvalid();
        }

        challengeStore.markCodeUsed(userId, command.code());
        challengeStore.invalidateChallenge(command.challengeId());

        AuthTokens tokens = new AuthTokens(
            accessTokenPort.generate(user),
            refreshTokenPort.generate(user, refreshTokenExpiryDays)
        );
        return new LoginResult.Success(tokens, user);
    }
}