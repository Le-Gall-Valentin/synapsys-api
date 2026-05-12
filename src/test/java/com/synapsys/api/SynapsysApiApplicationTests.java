package com.synapsys.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@TestPropertySource(properties = {
    "synapsys.jwt.secret=test-secret-key-at-least-32-chars-for-smoke!",
    "synapsys.jwt.expiry-minutes=15",
    "synapsys.refresh-token.expiry-days=30",
    "synapsys.cookie.secure=false",
    "spring.jpa.hibernate.ddl-auto=none"
})
class SynapsysApiApplicationTests {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void contextLoads() {
    }
}