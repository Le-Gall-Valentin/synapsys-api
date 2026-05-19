package com.synapsys.api;

import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
@ComponentScan(
    basePackages = "com.synapsys.api",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = ApplicationService.class
    )
)
public class SynapsysApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynapsysApiApplication.class, args);
    }
}
