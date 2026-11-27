package io.github.lucar94.jsql.internal.impl;

import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import io.github.lucar94.jsql.internal.DbInitializer;
import io.github.lucar94.jsql.internal.JsonRows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbInitializerImpl implements DbInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DbInitializerImpl.class);

    /** Rows sent to the database in a single JDBC batch. */
    static final int BATCH_SIZE = 1000;

    /**
     * Reads the rows twice: the first pass collects the columns needed by
     * {@code CREATE TABLE}, the second one inserts the rows in batches.
     * At no point are all the rows held in memory.
     */
    @Override
    public void initialize(JsonRows rows, String tableName, DSLContext dslContext) {
        ColumnCollector collector = new ColumnCollector();
        rows.forEach(collector::accept);
        List<String> columns = collector.columns();

        // All identifiers are quoted, so reserved words, leading digits and
        // special characters in the table name are safe
        Table<?> table = DSL.table(DSL.name(tableName));
        List<Field<String>> fields = columns.stream()
                .map(c -> DSL.field(DSL.name(c), SQLDataType.VARCHAR(255)))
                .toList();

        dslContext.createTable(table).columns(fields).execute();

        logger.debug("Created table {} with columns {}", tableName, columns);

        if (fields.isEmpty()) {
            return;
        }

        BatchInserter inserter = new BatchInserter(dslContext, table, fields, columns, collector);
        rows.forEach(inserter::add);
        inserter.flush();

        logger.debug("Inserted {} rows into table {}", inserter.inserted, tableName);
    }

    /**
     * Inserts rows with a prepared statement executed in batches of {@link #BATCH_SIZE}.
     * Values are passed as bind parameters, never concatenated into the SQL,
     * and keys missing from a row are bound as NULL.
     */
    private static final class BatchInserter {

        private final DSLContext dslContext;
        private final Table<?> table;
        private final List<Field<String>> fields;
        private final ColumnCollector collector;
        private final Map<String, Integer> columnIndex = new HashMap<>();

        private BatchBindStep batch;
        private int pending;
        private long inserted;

        BatchInserter(DSLContext dslContext, Table<?> table, List<Field<String>> fields,
                      List<String> columns, ColumnCollector collector) {
            this.dslContext = dslContext;
            this.table = table;
            this.fields = fields;
            this.collector = collector;
            for (int i = 0; i < columns.size(); i++) {
                columnIndex.put(columns.get(i), i);
            }
        }

        void add(Map<String, Object> row) {
            Object[] values = new Object[fields.size()];
            row.forEach((key, value) ->
                    values[columnIndex.get(collector.columnFor(key))] = value == null ? null : value.toString());

            if (batch == null) {
                batch = dslContext.batch(dslContext.insertInto(table, fields)
                        .values(Collections.nCopies(fields.size(), null)));
            }
            batch.bind(values);

            if (++pending == BATCH_SIZE) {
                flush();
            }
        }

        void flush() {
            if (pending > 0) {
                batch.execute();
                inserted += pending;
            }
            batch = null;
            pending = 0;
        }
    }

}
