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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class DbInitializerImpl implements DbInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DbInitializerImpl.class);

    @Override
    public void initialize(List<Map<String, Object>> json, String tableName, DSLContext dslContext) {
        Map<String, String> keyToColumn = mapKeysToColumns(json);
        List<String> columns = List.copyOf(new TreeSet<>(keyToColumn.values()));

        // All identifiers are quoted, so reserved words, leading digits and
        // special characters in the table name are safe
        Table<?> table = DSL.table(DSL.name(tableName));
        List<Field<String>> fields = columns.stream()
                .map(c -> DSL.field(DSL.name(c), SQLDataType.VARCHAR(255)))
                .toList();

        dslContext.createTable(table).columns(fields).execute();

        logger.debug("Created table {} with columns {}", tableName, columns);

        if (fields.isEmpty() || json.isEmpty()) {
            return;
        }

        insertRows(json, keyToColumn, columns, table, fields, dslContext);

        logger.debug("Inserted {} rows into table {}", json.size(), tableName);
    }

    /**
     * Maps every flattened JSON key to its column name, failing if two different keys
     * would end up in the same column (e.g. {@code "a.b"} and {@code "a_b"}, or
     * {@code "name"} and {@code "Name"}): one of the values would otherwise be lost.
     */
    private Map<String, String> mapKeysToColumns(List<Map<String, Object>> json) {
        Map<String, String> keyToColumn = new HashMap<>();
        Map<String, String> columnToKey = new HashMap<>();

        json.stream().flatMap(row -> row.keySet().stream()).forEach(key -> {
            if (keyToColumn.containsKey(key)) {
                return;
            }
            String column = DbUtils.toColumnName(key);
            String otherKey = columnToKey.putIfAbsent(column, key);
            if (otherKey != null) {
                throw new IllegalArgumentException(String.format(
                        "JSON keys \"%s\" and \"%s\" both map to column %s", otherKey, key, column));
            }
            keyToColumn.put(key, column);
        });

        return keyToColumn;
    }

    /**
     * Inserts all rows with a single prepared statement executed in batch.
     * Values are passed as bind parameters, never concatenated into the SQL,
     * and keys missing from a row are bound as NULL.
     */
    private void insertRows(List<Map<String, Object>> json, Map<String, String> keyToColumn,
                            List<String> columns, Table<?> table, List<Field<String>> fields,
                            DSLContext dslContext) {
        BatchBindStep batch = dslContext.batch(
                dslContext.insertInto(table, fields)
                        .values(Collections.nCopies(fields.size(), null)));

        for (Map<String, Object> row : json) {
            Map<String, Object> rowByColumn = new HashMap<>();
            row.forEach((key, value) -> rowByColumn.put(keyToColumn.get(key), value));

            Object[] values = columns.stream()
                    .map(rowByColumn::get)
                    .map(value -> value == null ? null : value.toString())
                    .toArray();
            batch.bind(values);
        }

        batch.execute();
    }

}
