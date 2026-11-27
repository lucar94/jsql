package io.github.lucar94.jsql.integration;

import io.github.lucar94.jsql.JSql;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests checking that JSON keys and table names that are not valid
 * unquoted SQL identifiers can still be used.
 */
class JSqlIdentifiersIntegrationTest {

    @Test
    @DisplayName("JSON keys that are SQL reserved words become queryable columns")
    void reservedWordKeys() {
        String json = """
                [
                  { "order": 1, "value": "a", "user": "alice", "group": "x" },
                  { "order": 2, "value": "b", "user": "bob", "group": "y" }
                ]
                """;

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Result<Record> results = jSql.execute(
                    select().from(jSql.getTable()).where(field(name("ORDER")).eq("2")));

            assertEquals(1, results.size());
            assertEquals("b", results.get(0).get("VALUE", String.class));
            assertEquals("bob", results.get(0).get("USER", String.class));
            assertEquals("y", results.get(0).get("GROUP", String.class));
        }
    }

    @Test
    @DisplayName("JSON keys starting with a digit become columns")
    void keyStartingWithDigit() {
        String json = """
                [ { "1st": "gold", "2nd": "silver" } ]
                """;

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Record row = jSql.execute(select().from(jSql.getTable())).get(0);

            assertEquals("gold", row.get("1ST", String.class));
            assertEquals("silver", row.get("2ND", String.class));
        }
    }

    @Test
    @DisplayName("Table names that are reserved words or contain spaces are allowed")
    void unusualTableNames() {
        String json = """
                [ { "id": 1 } ]
                """;

        for (String tableName : new String[]{"order", "my table"}) {
            try (JSql jSql = JSql.create().withTableName(tableName).withJson(json).build()) {
                assertEquals(1, jSql.execute(select().from(jSql.getTable())).size(), tableName);
            }
        }
    }

    @Test
    @DisplayName("Two JSON keys mapping to the same column are rejected instead of losing a value")
    void collidingKeys() {
        IllegalArgumentException flattened = assertThrows(IllegalArgumentException.class,
                () -> JSql.create().withJson("""
                        [ { "a": { "b": 1 }, "a_b": 2 } ]
                        """).build());
        assertEquals("JSON keys \"a.b\" and \"a_b\" both map to column A_B", flattened.getMessage());

        IllegalArgumentException caseOnly = assertThrows(IllegalArgumentException.class,
                () -> JSql.create().withJson("""
                        [ { "Name": "Alice" }, { "name": "Bob" } ]
                        """).build());
        assertEquals("JSON keys \"Name\" and \"name\" both map to column NAME", caseOnly.getMessage());
    }

    @Test
    @DisplayName("A JSON key with no valid character is rejected with a clear message")
    void keyWithoutValidCharacters() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JSql.create().withJson("""
                        [ { "!!!": 1 } ]
                        """).build());

        assertEquals("JSON key \"!!!\" has no valid character for a column name", ex.getMessage());
    }

    @Test
    @DisplayName("A failed build does not leave the table behind for the next instance")
    void failedBuildDoesNotLeakTable() {
        assertThrows(IllegalArgumentException.class,
                () -> JSql.create().withJson("[ { \"!!!\": 1 } ]").build());

        try (JSql jSql = JSql.create().withJson("[ { \"id\": 1 } ]").build()) {
            assertEquals(1, jSql.execute(select().from(jSql.getTable())).size());
        }
    }

}
