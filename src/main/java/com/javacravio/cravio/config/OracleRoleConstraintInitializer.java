package com.javacravio.cravio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Component
public class OracleRoleConstraintInitializer {

    private static final Logger log = LoggerFactory.getLogger(OracleRoleConstraintInitializer.class);

    private static final String ROLE_CONSTRAINT_SQL =
            "ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER','RESTAURANT','DELIVERY_PARTNER','ADMIN'))";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public OracleRoleConstraintInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureUsersRoleConstraint() {
        if (!isOracleDatabase() || !usersTableExists()) {
            return;
        }

        try {
            // Replace any legacy role check so RESTAURANT/DELIVERY_PARTNER values are accepted.
            List<String> constraintNames = findRoleRelatedCheckConstraints();

            for (String constraintName : constraintNames) {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT " + constraintName);
            }

            jdbcTemplate.execute(ROLE_CONSTRAINT_SQL);
            log.info("Applied users.role check constraint with supported roles");
        } catch (Exception ex) {
            log.warn("Could not update users.role check constraint automatically: {}", ex.getMessage());
        }
    }

    private List<String> findRoleRelatedCheckConstraints() {
        if (hasSearchConditionVcColumn()) {
            return jdbcTemplate.queryForList(
                    "SELECT constraint_name FROM user_constraints " +
                            "WHERE table_name = 'USERS' AND constraint_type = 'C' " +
                            "AND UPPER(search_condition_vc) LIKE '%ROLE%'",
                    String.class
            );
        }

        // Older Oracle versions may not expose SEARCH_CONDITION_VC.
        return jdbcTemplate.queryForList(
                "SELECT constraint_name FROM user_constraints " +
                        "WHERE table_name = 'USERS' AND constraint_type = 'C' " +
                        "AND constraint_name = 'CK_USERS_ROLE'",
                String.class
        );
    }

    private boolean hasSearchConditionVcColumn() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_tab_columns " +
                        "WHERE table_name = 'USER_CONSTRAINTS' AND column_name = 'SEARCH_CONDITION_VC'",
                Integer.class
        );
        return count != null && count > 0;
    }

    private boolean usersTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_tables WHERE table_name = 'USERS'",
                Integer.class
        );
        return count != null && count > 0;
    }

    private boolean isOracleDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("oracle");
        } catch (SQLException ex) {
            log.warn("Could not detect database vendor: {}", ex.getMessage());
            return false;
        }
    }
}

