package com.team.backend.config;

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

@Component
public class AutoBidSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AutoBidSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public AutoBidSchemaRepair(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "auto_bids")) {
                return;
            }

            if (!columnExists(connection, "auto_bids", "bid_step")) {
                log.info("Adding missing auto_bids.bid_step column");
                jdbcTemplate.execute("alter table auto_bids add column bid_step double precision");
            }

            int repaired = jdbcTemplate.update(
                    "update auto_bids set bid_step = 1.0 where bid_step is null or bid_step <= 0"
            );
            if (repaired > 0) {
                log.info("Backfilled {} auto_bids rows with default bid_step", repaired);
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        return objectExists(metaData.getTables(null, null, tableName, null))
                || objectExists(metaData.getTables(null, null, tableName.toUpperCase(), null))
                || objectExists(metaData.getTables(null, null, tableName.toLowerCase(), null));
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        return objectExists(metaData.getColumns(null, null, tableName, columnName))
                || objectExists(metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase()))
                || objectExists(metaData.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase()));
    }

    private boolean objectExists(ResultSet resultSet) throws Exception {
        try (ResultSet rs = resultSet) {
            return rs.next();
        }
    }
}
