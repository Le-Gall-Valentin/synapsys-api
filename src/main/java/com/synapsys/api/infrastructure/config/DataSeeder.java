package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.RegisterCommand;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.port.in.RegisterUseCase;
import com.synapsys.api.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SynapsysProperties properties;
    // kept only for existsAny() guard — creation is fully delegated to RegisterUseCase
    private final UserRepository userRepository;
    private final RegisterUseCase registerUseCase;

    public DataSeeder(SynapsysProperties properties,
                      UserRepository userRepository,
                      RegisterUseCase registerUseCase) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.registerUseCase = registerUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        SynapsysProperties.SeedProperties seed = properties.seed();
        if (seed == null || !seed.enabled() || userRepository.existsAny()) {
            return;
        }
        if (seed.password() == null || seed.password().isBlank()) {
            throw new IllegalStateException(
                "SYNAPSYS_SEED_PASSWORD must be set when seeding is enabled"
            );
        }
        registerUseCase.register(new RegisterCommand(
            seed.username(),
            seed.email(),
            seed.password(),
            Role.ADMIN
        ));
        log.info("Default admin user '{}' created", seed.username());
    }
}