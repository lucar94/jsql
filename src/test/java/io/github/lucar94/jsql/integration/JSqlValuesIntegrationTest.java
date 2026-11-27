package io.github.lucar94.jsql.integration;

import io.github.lucar94.jsql.JSql;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration tests checking that JSON values are stored verbatim,
 * whatever characters they contain.
 */
class JSqlValuesIntegrationTest {

    @Test
    @DisplayName("Values containing an apostrophe are stored verbatim")
    void valueWithApostrophe() {
        String json = """
                [ { "id": 1, "name": "O'Brien" } ]
                """;

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Result<Record> results = jSql.execute(select().from(jSql.getTable()));

            assertEquals(1, results.size());
            assertEquals("O'Brien", results.get(0).get("NAME", String.class));
        }
    }

    @Test
    @DisplayName("JSON null values are stored as SQL NULL")
    void nullValue() {
        String json = """
                [ { "id": 1, "name": null }, { "id": 2, "name": "Bob" } ]
                """;

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Result<Record> results = jSql.execute(
                    select().from(jSql.getTable()).where(field("NAME").isNull()));

            assertEquals(1, results.size());
            assertEquals("1", results.get(0).get("ID", String.class));
            assertNull(results.get(0).get("NAME"));
        }
    }

    @Test
    @DisplayName("SQL in a value is stored as text and never executed")
    void sqlInjectionAttempt() {
        String json = """
                [
                  { "id": 1, "name": "x'); DROP TABLE JSON_TABLE; --" },
                  { "id": 2, "name": "Bob" }
                ]
                """;

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Result<Record> results = jSql.execute(
                    select().from(jSql.getTable()).orderBy(field("ID")));

            assertEquals(2, results.size());
            assertEquals("x'); DROP TABLE JSON_TABLE; --", results.get(0).get("NAME", String.class));
            assertEquals("Bob", results.get(1).get("NAME", String.class));
        }
    }

    @Test
    @DisplayName("Rows missing some keys get NULL in those columns")
    void missingKeys() {
        String json = """
                [ { "id": 1, "name": "Alice" }, { "id": 2, "city": "Rome" } ]
                """;

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Result<Record> results = jSql.execute(
                    select().from(jSql.getTable()).orderBy(field("ID")));

            assertEquals(2, results.size());
            assertEquals("Alice", results.get(0).get("NAME", String.class));
            assertNull(results.get(0).get("CITY"));
            assertNull(results.get(1).get("NAME"));
            assertEquals("Rome", results.get(1).get("CITY", String.class));
        }
    }

}
