package io.github.lucar94.jsql.integration;

import io.github.lucar94.jsql.JSql;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the streaming load: rows are read one at a time
 * and inserted in batches of 1000.
 */
class JSqlStreamingIntegrationTest {

    private static String rows(int count) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            json.append(i == 0 ? "" : ",").append("{\"id\":").append(i).append(",\"name\":\"n").append(i).append("\"}");
        }
        return json.append(']').toString();
    }

    @ParameterizedTest(name = "{0} rows")
    @ValueSource(ints = {1, 999, 1000, 1001, 2500})
    @DisplayName("Every row is inserted, whatever its position relative to the batch boundaries")
    void allRowsAcrossBatches(int count) {
        try (JSql jSql = JSql.create().withJson(rows(count)).build()) {
            assertEquals(count, jSql.execute(select().from(jSql.getTable())).size());

            Result<Record> last = jSql.execute(
                    select().from(jSql.getTable()).where(field("ID").eq(String.valueOf(count - 1))));
            assertEquals("n" + (count - 1), last.get(0).get("NAME", String.class));
        }
    }

    @Test
    @DisplayName("A key appearing only in a late row still becomes a column")
    void keyOnlyInLateRow() {
        String json = rows(1500).replaceFirst("\\{\"id\":1400,", "{\"id\":1400,\"late\":\"here\",");

        try (JSql jSql = JSql.create().withJson(json).build()) {
            Result<Record> withLate = jSql.execute(
                    select().from(jSql.getTable()).where(field("LATE").isNotNull()));

            assertEquals(1, withLate.size());
            assertEquals("1400", withLate.get(0).get("ID", String.class));
            assertEquals("here", withLate.get(0).get("LATE", String.class));
        }
    }

    @Test
    @DisplayName("Rows are streamed from a file too")
    void fromFile(@TempDir Path dir) throws IOException {
        File file = dir.resolve("rows.json").toFile();
        Files.writeString(file.toPath(), rows(2500));

        try (JSql jSql = JSql.create().withJson(file).build()) {
            assertEquals(2500, jSql.execute(select().from(jSql.getTable())).size());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "[{\"a\":1},{\"a\":2}",          // array never closed
            "[{\"a\":1},{\"a\":}]",          // invalid element in the middle
            "[{\"a\":1},]"                   // trailing comma
    })
    @DisplayName("Malformed arrays are rejected before anything is inserted")
    void malformedArray(String json) {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JSql.create().withJson(json).build());
        assertEquals("Failed to parse JSON string", ex.getMessage().replace(": unexpected end of input", ""));

        // The failed build left nothing behind
        try (JSql jSql = JSql.create().withJson("[{\"a\":1}]").build()) {
            assertEquals(1, jSql.execute(select().from(jSql.getTable())).size());
        }
    }

    @Test
    @DisplayName("A malformed file is reported as a parse error")
    void malformedFile(@TempDir Path dir) throws IOException {
        File file = dir.resolve("bad.json").toFile();
        Files.writeString(file.toPath(), "[{\"a\":1},{\"a\":");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JSql.create().withJson(file).build());
        assertEquals("Failed to parse JSON string", ex.getMessage().replace(": unexpected end of input", ""));
    }

}
