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

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS repair_order (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "user_id BIGINT NOT NULL," +
                            "handler_id BIGINT DEFAULT NULL," +
                            "title VARCHAR(100) NOT NULL," +
                            "description TEXT NOT NULL," +
                            "contact_phone VARCHAR(20) DEFAULT NULL," +
                            "status VARCHAR(40) NOT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "INDEX idx_repair_order_user_id (user_id)," +
                            "INDEX idx_repair_order_handler_id (handler_id)," +
                            "INDEX idx_repair_order_status (status)," +
                            "CONSTRAINT fk_repair_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id)," +
                            "CONSTRAINT fk_repair_order_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id)" +
                            ")"
            );
            log.info("database migration success: ensured table repair_order");
        } catch (DataAccessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("already exists")) {
                log.info("database migration skipped: table repair_order already exists");
            } else {
                log.warn("database migration warning (repair_order): {}", message);
            }
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS repair_order_image (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "order_id BIGINT NOT NULL," +
                            "image_path VARCHAR(255) NOT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_repair_order_image_order_id (order_id)," +
                            "CONSTRAINT fk_repair_order_image_order FOREIGN KEY (order_id) REFERENCES repair_order(id) ON DELETE CASCADE" +
                            ")"
            );
            log.info("database migration success: ensured table repair_order_image");
        } catch (DataAccessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("already exists")) {
                log.info("database migration skipped: table repair_order_image already exists");
            } else {
                log.warn("database migration warning (repair_order_image): {}", message);
            }
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS repair_order_flow (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "order_id BIGINT NOT NULL," +
                            "from_status VARCHAR(40) DEFAULT NULL," +
                            "to_status VARCHAR(40) NOT NULL," +
                            "remark VARCHAR(255) DEFAULT NULL," +
                            "operator_id BIGINT DEFAULT NULL," +
                            "operator_name VARCHAR(50) DEFAULT NULL," +
                            "operator_role VARCHAR(20) DEFAULT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_repair_order_flow_order_id (order_id)," +
                            "CONSTRAINT fk_repair_order_flow_order FOREIGN KEY (order_id) REFERENCES repair_order(id) ON DELETE CASCADE" +
                            ")"
            );
            log.info("database migration success: ensured table repair_order_flow");
        } catch (DataAccessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("already exists")) {
                log.info("database migration skipped: table repair_order_flow already exists");
            } else {
                log.warn("database migration warning (repair_order_flow): {}", message);
            }
        }
    }
}
