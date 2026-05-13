package com.synapsys.api.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI synapsysOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Synapsys API")
                        .version("1.0.0")
                        .description("Documentation de l'API Synapsys"));
    }
}
