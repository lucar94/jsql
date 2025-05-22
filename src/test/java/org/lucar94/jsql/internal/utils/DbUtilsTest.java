package org.lucar94.jsql.internal.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbUtilsTest {

    @Test
    void testSanitizeColumnNameReplacesInvalidCharactersWithUnderscore() {
        String result = DbUtils.sanitizeColumnName("col@name!");
        assertEquals("col_name", result);
    }

    @Test
    void testSanitizeColumnNameTrimsTrailingUnderscores() {
        String result = DbUtils.sanitizeColumnName("column___");
        assertEquals("column", result);
    }

    @Test
    void testSanitizeColumnNameThrowsExceptionForNullInput() {
        assertThrows(IllegalArgumentException.class, () -> DbUtils.sanitizeColumnName(null));
    }

    @Test
    void testSanitizeColumnNameThrowsExceptionForEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> DbUtils.sanitizeColumnName(""));
    }

    @Test
    void testSanitizeColumnNameHandlesValidColumnNameWithoutChanges() {
        String result = DbUtils.sanitizeColumnName("valid_column_name");
        assertEquals("valid_column_name", result);
    }

    @Test
    void testSanitizeColumnNameHandlesOnlyInvalidCharacters() {
        String result = DbUtils.sanitizeColumnName("!!!");
        assertEquals("", result);
    }

}