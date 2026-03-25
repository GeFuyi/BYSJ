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

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_category (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "code VARCHAR(32) NOT NULL UNIQUE," +
                            "name VARCHAR(50) NOT NULL," +
                            "sort INT NOT NULL DEFAULT 0," +
                            "status TINYINT NOT NULL DEFAULT 1" +
                            ")"
            );
            log.info("database migration success: ensured table service_category");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (service_category): {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS convenience_service (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "provider_id BIGINT NOT NULL," +
                            "name VARCHAR(100) NOT NULL," +
                            "category_code VARCHAR(32) NOT NULL," +
                            "summary VARCHAR(255) NOT NULL," +
                            "description TEXT NOT NULL," +
                            "contact_name VARCHAR(50) NOT NULL," +
                            "contact_phone VARCHAR(20) NOT NULL," +
                            "address VARCHAR(255) DEFAULT NULL," +
                            "cover_image_path VARCHAR(255) DEFAULT NULL," +
                            "service_status VARCHAR(20) NOT NULL DEFAULT 'RESERVABLE'," +
                            "audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                            "audit_reason VARCHAR(255) DEFAULT NULL," +
                            "reviewed_by BIGINT DEFAULT NULL," +
                            "reviewed_at DATETIME DEFAULT NULL," +
                            "max_capacity INT NOT NULL DEFAULT 50," +
                            "current_booked INT NOT NULL DEFAULT 0," +
                            "avg_score DECIMAL(4,2) NOT NULL DEFAULT 0.00," +
                            "score_count INT NOT NULL DEFAULT 0," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "INDEX idx_convenience_service_provider (provider_id)," +
                            "INDEX idx_convenience_service_audit_status (audit_status)," +
                            "INDEX idx_convenience_service_service_status (service_status)," +
                            "INDEX idx_convenience_service_category (category_code)," +
                            "CONSTRAINT fk_convenience_service_provider FOREIGN KEY (provider_id) REFERENCES sys_user(id)," +
                            "CONSTRAINT fk_convenience_service_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id)" +
                            ")"
            );
            log.info("database migration success: ensured table convenience_service");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (convenience_service): {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_image (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "service_id BIGINT NOT NULL," +
                            "image_path VARCHAR(255) NOT NULL," +
                            "sort_no INT NOT NULL DEFAULT 0," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_service_image_service_id (service_id)," +
                            "CONSTRAINT fk_service_image_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE" +
                            ")"
            );
            log.info("database migration success: ensured table service_image");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (service_image): {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_audit_log (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "service_id BIGINT NOT NULL," +
                            "from_audit_status VARCHAR(20) NOT NULL," +
                            "to_audit_status VARCHAR(20) NOT NULL," +
                            "action VARCHAR(20) NOT NULL," +
                            "reason VARCHAR(255) DEFAULT NULL," +
                            "reviewer_id BIGINT NOT NULL," +
                            "reviewer_name VARCHAR(50) DEFAULT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_service_audit_log_service_id (service_id)," +
                            "CONSTRAINT fk_service_audit_log_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE" +
                            ")"
            );
            log.info("database migration success: ensured table service_audit_log");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (service_audit_log): {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_booking (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "service_id BIGINT NOT NULL," +
                            "user_id BIGINT NOT NULL," +
                            "contact_name VARCHAR(50) NOT NULL," +
                            "contact_phone VARCHAR(20) NOT NULL," +
                            "remark VARCHAR(255) DEFAULT NULL," +
                            "status VARCHAR(20) NOT NULL DEFAULT 'BOOKED'," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "INDEX idx_service_booking_service_id (service_id)," +
                            "INDEX idx_service_booking_user_id (user_id)," +
                            "INDEX idx_service_booking_status (status)," +
                            "CONSTRAINT fk_service_booking_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE," +
                            "CONSTRAINT fk_service_booking_user FOREIGN KEY (user_id) REFERENCES sys_user(id)" +
                            ")"
            );
            log.info("database migration success: ensured table service_booking");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (service_booking): {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS service_review (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "service_id BIGINT NOT NULL," +
                            "user_id BIGINT NOT NULL," +
                            "rating TINYINT NOT NULL," +
                            "content VARCHAR(500) DEFAULT NULL," +
                            "reviewer_name VARCHAR(50) DEFAULT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "UNIQUE KEY uk_service_review_unique (service_id, user_id)," +
                            "INDEX idx_service_review_service_id (service_id)," +
                            "CONSTRAINT fk_service_review_service FOREIGN KEY (service_id) REFERENCES convenience_service(id) ON DELETE CASCADE," +
                            "CONSTRAINT fk_service_review_user FOREIGN KEY (user_id) REFERENCES sys_user(id)" +
                            ")"
            );
            log.info("database migration success: ensured table service_review");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (service_review): {}", ex.getMessage());
        }

        try {
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('HOUSEKEEPING', '家政保洁', 1, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('HOME_REPAIR', '家电维修', 2, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('ELDER_CARE', '养老照护', 3, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('CHILD_CARE', '托育陪护', 4, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('PET_CARE', '宠物服务', 5, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('ERRAND', '跑腿代办', 6, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('IT_SUPPORT', '数码协助', 7, 1)");
            jdbcTemplate.execute("INSERT IGNORE INTO service_category (code, name, sort, status) VALUES ('COMMUNITY_CLASS', '社区课堂', 8, 1)");
        } catch (DataAccessException ex) {
            log.warn("database migration warning (init service_category data): {}", ex.getMessage());
        }
    }
}
