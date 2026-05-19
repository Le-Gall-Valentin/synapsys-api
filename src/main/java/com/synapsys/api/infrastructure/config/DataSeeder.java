package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.CreateUserCommand;
import com.synapsys.api.auth.domain.model.Role;
import com.synapsys.api.auth.domain.port.out.PasswordHasherPort;
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
    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasher;

    public DataSeeder(SynapsysProperties properties,
                      UserRepository userRepository,
                      PasswordHasherPort passwordHasher) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        SynapsysProperties.SeedProperties seed = properties.seed();
        if (seed == null || seed.password() == null || seed.password().isBlank()) {
            throw new IllegalStateException(
                "SYNAPSYS_SEED_PASSWORD must be set — the initial SUPER_ADMIN cannot be created without it"
            );
        }
        if (!userRepository.isEmpty()) {
            log.info("Database already has users, skipping seed");
            return;
        }
        String hash = passwordHasher.hash(seed.password());
        userRepository.save(new CreateUserCommand(seed.username(), seed.email(), hash, Role.SUPER_ADMIN));
        log.info("Default SUPER_ADMIN '{}' created", seed.username());
    }
}