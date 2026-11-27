package io.github.lucar94.jsql.internal.utils;

import java.util.Locale;

public class DbUtils {

    private DbUtils() {}

    public static String sanitizeColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        String first = columnName.replaceAll("[^a-zA-Z0-9_]", "_");
        return first.replaceAll("_+$", "");
    }

    /**
     * Maps a flattened JSON key to the name of its column: sanitized and upper-cased,
     * so it can be referenced both unquoted ({@code field("id")}) and quoted
     * ({@code field(name("ID"))}).
     *
     * @throws IllegalArgumentException if the key is null or empty, or has no valid character left
     */
    public static String toColumnName(String jsonKey) {
        String column = sanitizeColumnName(jsonKey).toUpperCase(Locale.ROOT);
        if (column.isEmpty()) {
            throw new IllegalArgumentException(
                    "JSON key \"" + jsonKey + "\" has no valid character for a column name");
        }
        return column;
    }

}
