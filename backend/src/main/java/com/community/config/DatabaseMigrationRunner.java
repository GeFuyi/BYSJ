package com.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DatabaseMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void migrate() {
        bootstrapSchema();
        ensureColumns();
        seedServiceCategories();
    }

    private void bootstrapSchema() {
        Resource schema = resourceLoader.getResource("classpath:schema.sql");
        try {
            String sql = StreamUtils.copyToString(schema.getInputStream(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                jdbcTemplate.execute(trimmed);
            }
            log.info("database migration success: schema bootstrap done");
        } catch (IOException ex) {
            log.error("database migration failed: cannot read schema.sql", ex);
            throw new IllegalStateException("cannot read schema.sql", ex);
        } catch (DataAccessException ex) {
            log.error("database migration failed while executing schema.sql: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private void ensureColumns() {
        alterIgnoreDuplicate(
                "ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) NOT NULL DEFAULT '15138114047' AFTER password",
                "database migration success: added sys_user.phone",
                "database migration skipped: sys_user.phone already exists"
        );

        executeIgnoreError(
                "UPDATE sys_user SET phone = '15138114047' WHERE phone IS NULL OR phone = ''",
                "database migration success: default phone filled",
                "database migration warning (fill default phone): {}"
        );

        alterIgnoreDuplicate(
                "ALTER TABLE sys_user ADD COLUMN avatar_path VARCHAR(255) DEFAULT NULL AFTER nickname",
                "database migration success: added sys_user.avatar_path",
                "database migration skipped: sys_user.avatar_path already exists"
        );

        alterIgnoreDuplicate(
                "ALTER TABLE chat_group_member ADD COLUMN muted TINYINT NOT NULL DEFAULT 0 AFTER role",
                "database migration success: added chat_group_member.muted",
                "database migration skipped: chat_group_member.muted already exists"
        );

        alterIgnoreDuplicate(
                "ALTER TABLE chat_group ADD COLUMN announcement VARCHAR(500) DEFAULT NULL AFTER owner_id",
                "database migration success: added chat_group.announcement",
                "database migration skipped: chat_group.announcement already exists"
        );

        alterIgnoreDuplicate(
                "ALTER TABLE chat_group ADD COLUMN announcement_version BIGINT NOT NULL DEFAULT 0 AFTER announcement",
                "database migration success: added chat_group.announcement_version",
                "database migration skipped: chat_group.announcement_version already exists"
        );

        alterIgnoreDuplicate(
                "ALTER TABLE chat_group ADD COLUMN announcement_updated_at DATETIME DEFAULT NULL AFTER announcement_version",
                "database migration success: added chat_group.announcement_updated_at",
                "database migration skipped: chat_group.announcement_updated_at already exists"
        );
    }

    private void seedServiceCategories() {
        try {
            upsertServiceCategory("HOUSEKEEPING", "家政服务", 1);
            upsertServiceCategory("HOME_REPAIR", "上门维修", 2);
            upsertServiceCategory("ELDER_CARE", "助老照护", 3);
            upsertServiceCategory("CHILD_CARE", "儿童托管", 4);
            upsertServiceCategory("PET_CARE", "宠物照看", 5);
            upsertServiceCategory("ERRAND", "跑腿代办", 6);
            upsertServiceCategory("IT_SUPPORT", "数码维修", 7);
            upsertServiceCategory("COMMUNITY_CLASS", "社区课堂", 8);
        } catch (DataAccessException ex) {
            log.warn("database migration warning (init service_category data): {}", ex.getMessage());
        }
    }

    private void upsertServiceCategory(String code, String name, int sort) {
        jdbcTemplate.update(
                "INSERT INTO service_category (code, name, sort, status) VALUES (?, ?, ?, 1) " +
                        "ON DUPLICATE KEY UPDATE name = VALUES(name), sort = VALUES(sort), status = VALUES(status)",
                code, name, sort
        );
    }

    private void alterIgnoreDuplicate(String sql, String successLog, String duplicateLog) {
        try {
            jdbcTemplate.execute(sql);
            log.info(successLog);
        } catch (DataAccessException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("Duplicate column name")) {
                log.info(duplicateLog);
            } else {
                log.warn("database migration warning: {}", message);
            }
        }
    }

    private void executeIgnoreError(String sql, String successLog, String warnLogFormat) {
        try {
            jdbcTemplate.execute(sql);
            log.info(successLog);
        } catch (DataAccessException ex) {
            log.warn(warnLogFormat, ex.getMessage());
        }
    }
}
