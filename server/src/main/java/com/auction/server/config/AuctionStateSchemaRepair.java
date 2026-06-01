package com.auction.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

@Component
public class AuctionStateSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuctionStateSchemaRepair.class);
    private static final String EXPECTED_STATE_CHECK =
            "STATE IN('DRAFT','SCHEDULED','ACTIVE','FINISHED','CANCELLED','REJECTED','DELETED')";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public AuctionStateSchemaRepair(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!isH2(connection) || !tableExists(connection, "auctions") || !columnExists(connection, "auctions", "state")) {
                return;
            }

            List<String> checkClauses = jdbcTemplate.query(
                    """
                    select cc.check_clause
                    from information_schema.table_constraints tc
                    join information_schema.check_constraints cc
                      on tc.constraint_catalog = cc.constraint_catalog
                     and tc.constraint_schema = cc.constraint_schema
                     and tc.constraint_name = cc.constraint_name
                    where upper(tc.table_name) = 'AUCTIONS'
                      and upper(tc.constraint_type) = 'CHECK'
                    """,
                    (rs, rowNum) -> rs.getString("check_clause")
            );

            boolean alreadyCompatible = checkClauses.stream()
                    .filter(clause -> clause != null && !clause.isBlank())
                    .map(this::normalizeClause)
                    .anyMatch(normalized -> normalized.contains(EXPECTED_STATE_CHECK));

            if (alreadyCompatible) {
                return;
            }

            List<String> constraintNames = jdbcTemplate.query(
                    """
                    select constraint_name
                    from information_schema.table_constraints
                    where upper(table_name) = 'AUCTIONS'
                      and upper(constraint_type) = 'CHECK'
                    """,
                    (rs, rowNum) -> rs.getString("constraint_name")
            );

            for (String constraintName : constraintNames) {
                jdbcTemplate.execute("alter table auctions drop constraint " + constraintName);
                log.info("Dropped outdated auctions check constraint {}", constraintName);
            }

            jdbcTemplate.execute(
                    "alter table auctions add constraint auctions_state_check " +
                            "check (state in ('DRAFT','SCHEDULED','ACTIVE','FINISHED','CANCELLED','REJECTED','DELETED'))"
            );
            log.info("Recreated auctions.state check constraint with REJECTED and DELETED states");
        }
    }

    private boolean isH2(Connection connection) throws Exception {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toUpperCase(Locale.ROOT).contains("H2");
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        return objectExists(metaData.getTables(null, null, tableName, null))
                || objectExists(metaData.getTables(null, null, tableName.toUpperCase(Locale.ROOT), null))
                || objectExists(metaData.getTables(null, null, tableName.toLowerCase(Locale.ROOT), null));
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        return objectExists(metaData.getColumns(null, null, tableName, columnName))
                || objectExists(metaData.getColumns(null, null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT)))
                || objectExists(metaData.getColumns(null, null, tableName.toLowerCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT)));
    }

    private boolean objectExists(ResultSet resultSet) throws Exception {
        try (ResultSet rs = resultSet) {
            return rs.next();
        }
    }

    private String normalizeClause(String clause) {
        return clause.replaceAll("\\s+", "")
                .replace("\"", "")
                .toUpperCase(Locale.ROOT);
    }
}

