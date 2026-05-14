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
        if (seed == null || !seed.enabled() || userRepository.existsAny()) {
            return;
        }
        if (seed.password() == null || seed.password().isBlank()) {
            throw new IllegalStateException(
                "SYNAPSYS_SEED_PASSWORD must be set when seeding is enabled"
            );
        }
        userRepository.save(new CreateUserCommand(
            seed.username(),
            seed.email(),
            passwordHasher.hash(seed.password()),
            Role.ADMIN
        ));
        log.info("Default admin user '{}' created", seed.username());
    }
}