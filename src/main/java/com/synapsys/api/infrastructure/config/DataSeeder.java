package com.synapsys.api.infrastructure.config;

import com.synapsys.api.identity.application.port.in.SeedUseCase;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DataSeeder {

    private final SynapsysProperties properties;
    private final SeedUseCase seedUseCase;

    public DataSeeder(SynapsysProperties properties, SeedUseCase seedUseCase) {
        this.properties = properties;
        this.seedUseCase = seedUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        SynapsysProperties.SeedProperties seed = properties.seed();
        if (seed == null || seed.password() == null || seed.password().isBlank()) {
            throw new IllegalStateException(
                "SYNAPSYS_SEED_PASSWORD must be set — the initial SUPER_ADMIN cannot be created without it"
            );
        }
        seedUseCase.seedInitialSuperAdmin(seed.username(), seed.email().toLowerCase(Locale.ROOT), seed.password());
    }
}