package io.github.lucar94.jsql.internal.utils;

public class DbUtils {

    private DbUtils() {}

    public static String sanitizeColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        String first = columnName.replaceAll("[^a-zA-Z0-9_]", "_");
        return first.replaceAll("_+$", "");
    }

}
