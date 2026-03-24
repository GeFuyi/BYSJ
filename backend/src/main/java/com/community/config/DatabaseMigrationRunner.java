package com.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class DatabaseMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        try {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) NOT NULL DEFAULT '15138114047' AFTER password");
            log.info("database migration success: added column sys_user.phone");
        } catch (DataAccessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("Duplicate column name")) {
                log.info("database migration skipped: column sys_user.phone already exists");
            } else {
                log.warn("database migration warning (add phone): {}", message);
            }
        }

        try {
            jdbcTemplate.execute("UPDATE sys_user SET phone = '15138114047' WHERE phone IS NULL OR phone = ''");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (fill default phone): {}", ex.getMessage());
        }
    }
}
