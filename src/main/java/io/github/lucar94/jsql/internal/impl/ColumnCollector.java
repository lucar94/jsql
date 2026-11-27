package io.github.lucar94.jsql.internal.impl;

import io.github.lucar94.jsql.internal.utils.DbUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Collects the columns of the table while the rows are read for the first time,
 * mapping every flattened JSON key to its column name.
 */
class ColumnCollector {

    private final Map<String, String> keyToColumn = new HashMap<>();
    private final Map<String, String> columnToKey = new HashMap<>();

    /**
     * Registers the keys of a row.
     *
     * @throws IllegalArgumentException if two different keys map to the same column
     *         (e.g. {@code "a.b"} and {@code "a_b"}, or {@code "name"} and {@code "Name"}):
     *         one of the values would otherwise be lost
     */
    void accept(Map<String, Object> row) {
        for (String key : row.keySet()) {
            if (keyToColumn.containsKey(key)) {
                continue;
            }
            String column = DbUtils.toColumnName(key);
            String otherKey = columnToKey.putIfAbsent(column, key);
            if (otherKey != null) {
                throw new IllegalArgumentException(String.format(
                        "JSON keys \"%s\" and \"%s\" both map to column %s", otherKey, key, column));
            }
            keyToColumn.put(key, column);
        }
    }

    /** The column names, sorted. */
    List<String> columns() {
        return List.copyOf(new TreeSet<>(keyToColumn.values()));
    }

    /**
     * @throws IllegalStateException if the key was not seen while collecting the columns
     */
    String columnFor(String key) {
        String column = keyToColumn.get(key);
        if (column == null) {
            throw new IllegalStateException("JSON key \"" + key + "\" not found while reading the JSON "
                    + "the first time: was the file modified while loading it?");
        }
        return column;
    }

}
