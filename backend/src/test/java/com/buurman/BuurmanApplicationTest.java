package com.buurman;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BuurmanApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the entire Spring application context starts successfully:
        // - All beans are created and wired
        // - @ConfigurationProperties are bound
        // - Flyway migrations run against Testcontainers PostgreSQL
        // - JOOQ DSLContext is configured
    }
}
