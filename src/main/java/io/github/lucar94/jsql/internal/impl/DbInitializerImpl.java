package io.github.lucar94.jsql.internal.impl;

import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import io.github.lucar94.jsql.internal.DbInitializer;
import io.github.lucar94.jsql.internal.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

        if (columns.isEmpty() || json.isEmpty()) {
            return;
        }

        insertRows(json, tableName, new ArrayList<>(columns), dslContext);

        logger.debug("Inserted {} rows into table {}", json.size(), tableName);
    }

    /**
     * Inserts all rows with a single prepared statement executed in batch.
     * Values are passed as bind parameters, never concatenated into the SQL,
     * and keys missing from a row are bound as NULL.
     */
    private void insertRows(List<Map<String, Object>> json, String tableName,
                            List<String> columns, DSLContext dslContext) {
        // Unquoted names, to match the unquoted identifiers used in the DDL above
        Table<?> table = DSL.table(DSL.unquotedName(tableName));
        List<Field<String>> fields = columns.stream()
                .map(c -> DSL.field(DSL.unquotedName(c), SQLDataType.VARCHAR))
                .toList();

        BatchBindStep batch = dslContext.batch(
                dslContext.insertInto(table, fields)
                        .values(Collections.nCopies(fields.size(), null)));

        for (Map<String, Object> row : json) {
            Map<String, Object> sanitizedRow = new HashMap<>();
            row.forEach((key, value) -> sanitizedRow.put(DbUtils.sanitizeColumnName(key), value));

            Object[] values = columns.stream()
                    .map(sanitizedRow::get)
                    .map(value -> value == null ? null : value.toString())
                    .toArray();
            batch.bind(values);
        }

        batch.execute();
    }

}
