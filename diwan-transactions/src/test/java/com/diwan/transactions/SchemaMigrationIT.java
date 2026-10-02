package com.diwan.transactions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the service against an EMPTY MySQL 8.4: every Flyway migration must apply, and Hibernate
 * (ddl-auto=validate) must accept the resulting schema. Fails when an entity changes without a migration.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("DEV")
@Testcontainers
class SchemaMigrationIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsApplyAndEntitiesMatchTheSchema() {
        Integer applied = jdbc.queryForObject("select count(*) from flyway_schema_history where success = 1", Integer.class);
        Integer failed = jdbc.queryForObject("select count(*) from flyway_schema_history where success = 0", Integer.class);
        assertTrue(applied != null && applied >= 1, "no migration was applied");
        assertEquals(0, failed);
    }
}
