package org.lucar94.jsql.internal.impl;

import org.jooq.DSLContext;
import org.lucar94.jsql.internal.DbInitializer;
import org.lucar94.jsql.internal.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DbInitializerImpl implements DbInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DbInitializerImpl.class);

    @Override
    public void initialize(List<Map<String, Object>> json, String tableName, DSLContext dslContext) {
        Set<String> columns = json.stream()
                .flatMap(map -> map.keySet().stream())
                .map(DbUtils::sanitizeColumnName)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        StringBuilder ddlBuilder = new StringBuilder("CREATE TABLE %s (");
        columns.forEach(c -> ddlBuilder.append(c).append(" VARCHAR(255), "));
        ddlBuilder.append(")");

        String ddl = ddlBuilder.toString().replaceAll(",(?=[^,]*$)", "");
        dslContext.execute(String.format(ddl, tableName));

        logger.debug("Created table {} with columns {}", tableName, columns);

        json.forEach(row -> {
            String insert = "INSERT INTO %s (%s) VALUES (%s)";
            String cols = row.keySet().stream()
                    .map(DbUtils::sanitizeColumnName)
                    .collect(Collectors.joining(","));
            String data = row.values().stream()
                    .map(value -> "'" + value.toString() + "'")
                    .collect(Collectors.joining(","));

            dslContext.execute(String.format(insert, tableName, cols, data));
        });

        logger.debug("Inserted {} rows into table {}", json.size(), tableName);
    }

}
