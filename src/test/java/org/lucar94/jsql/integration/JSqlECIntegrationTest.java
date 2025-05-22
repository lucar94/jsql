package org.lucar94.jsql.integration;

import org.junit.jupiter.api.Test;
import org.lucar94.jsql.JSql;
import org.mockito.Mockito;

import java.io.File;

import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This class contains Error Case integration tests for the JSql class.
 * It checks the behavior of the JSql class when it is not properly configured.
 */
class JSqlECIntegrationTest {

    private static final String BAD_INPUT_JSON = """
            {
                "id: 1,
                "name": "Alice",
                "age": 45
            """;

    /**
     * This test checks if an exception is thrown when trying to build the JSql instance without providing JSON data.
     */
    @Test
    void testIntegrationEC_001() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> JSql.create().build());

        assertEquals("JSON data not provided. Use withJson() to set JSON data.", ex.getMessage());
    }

    /**
     * This test checks if an exception is thrown when trying to set a JSON file after setting a JSON string.
     */
    @Test
    void testIntegrationEC_002() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JSql.create()
                        .withJson("{}")
                        .withJson(Mockito.mock(File.class)));

        assertEquals("Cannot set JSON file after setting JSON string", ex.getMessage());
    }

    /**
     * This test checks if an exception is thrown when trying to set a JSON string after setting a JSON file.
     */
    @Test
    void testIntegrationEC_003() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JSql.create()
                        .withJson(Mockito.mock(File.class))
                        .withJson("{}"));

        assertEquals("Cannot set JSON string after setting JSON file", ex.getMessage());
    }

    /**
     * This test checks if an exception is thrown when trying to execute a query before building the JSql instance.
     */
    @Test
    void testIntegrationEC_004() {
        JSql jSql = JSql.create()
                .withJson("{}");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> jSql.execute(select()));

        assertEquals("JSql not built. Call build() before executing queries.", ex.getMessage());
    }

    /**
     * This test checks if an exception is thrown when trying to access the table before building the JSql instance.
     */
    @Test
    void testIntegrationEC_005() {
        JSql jSql = JSql.create()
                .withJson("{}");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                jSql::getTable);

        assertEquals("JSql not built. Call build() before accessing the table.", ex.getMessage());
    }

    /**
     * This test checks if an exception is thrown when trying to read a malformed JSON string.
     */
    @Test
    void testIntegrationEC_006() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JSql.create()
                        .withJson(BAD_INPUT_JSON)
                        .build());

        assertEquals("Failed to parse JSON string", ex.getMessage());
    }

    /**
     * This test checks if an exception is thrown when trying to read a non-existing JSON file.
     */
    @Test
    void testIntegrationEC_007() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JSql.create()
                        .withJson(new File("/not-existing-file.json"))
                        .build());

        assertEquals("Failed to read JSON file: /not-existing-file.json", ex.getMessage());
    }

}
