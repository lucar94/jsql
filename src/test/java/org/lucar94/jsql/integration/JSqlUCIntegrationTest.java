package org.lucar94.jsql.integration;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lucar94.jsql.JSql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lucar94.jsql.internal.DbConstants.H2_PASSWORD;
import static org.lucar94.jsql.internal.DbConstants.H2_URL;
import static org.lucar94.jsql.internal.DbConstants.H2_USER;

/**
 * This class contains Use Case integration tests for the JSql class.
 * It checks the behavior of the JSql class with different JSON inputs and configurations.
 */
class JSqlUCIntegrationTest {

    private static final String INPUT_SINGLE_JSON = """
            {
                "id": 1,
                "name": "Alice",
                "age": 45
            }
            """;

    private static final String INPUT_ONE_LEVEL_JSON = """
            [
                {
                  "id": 1,
                  "name": "Alice",
                  "age": 45
                },
                {
                  "id": 2,
                  "name": "Bob",
                  "age": 30
                },
                {
                  "id": 3,
                  "name": "Charlie",
                  "age": 99
                },
                {
                  "id": 4,
                  "name": "Dan",
                  "age": 18
                }
            ]
            """;

    @AfterEach
    void tearDown() throws SQLException {
        Connection conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
        String sql = "DROP TABLE IF EXISTS JSON_TABLE";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error dropping table: " + e.getMessage());
        }
    }

    /**
     * This test checks the integration of JSql with a one-level JSON input.
     * It creates a JSql instance, executes a query to select a record with id 2,
     * and verifies the result.
     */
    @Test
    void testIntegrationUC_001() {
        JSql jSql = JSql.create()
                .withJson(INPUT_ONE_LEVEL_JSON)
                .build();

        Select<Record> select = select()
                .from(jSql.getTable())
                .where(field("id").eq("2"));

        Result<Record> results = jSql.execute(select);

        assertEquals(1, results.size());

        Record record = results.get(0);
        assertEquals(2, record.get(DSL.field(DSL.name("ID")), Integer.class));
        assertEquals("Bob", record.get("NAME", String.class));
        assertEquals("30", record.get("AGE"));

        jSql.close();
    }

    /**
     * This test checks the integration of JSql with a one-level JSON input.
     * It creates a JSql instance using a custom table name, executes a query to select a record with id 1,
     * and verifies the result.
     */
    @Test
    void testIntegrationUC_002() {
        JSql jSql = JSql.create()
                .withTableName("TEST_TABLE")
                .withJson(INPUT_ONE_LEVEL_JSON)
                .build();

        Select<Record> select = select()
                .from(jSql.getTable())
                .where(field("id").eq("1"));

        Result<Record> results = jSql.execute(select);

        assertEquals(1, results.size());

        Record record = results.get(0);
        assertEquals(1, record.get(DSL.field(DSL.name("ID")), Integer.class));
        assertEquals("Alice", record.get("NAME", String.class));
        assertEquals("45", record.get("AGE"));

        jSql.close();
    }

    /**
     * This test checks the integration of JSql with a complex JSON input from a file.
     * It creates a JSql instance using a JSON file, executes a query to select all records,
     * and verifies the result.
     */
    @Test
    void testIntegrationUC_003() {
        JSql jSql = JSql.create()
                .withJson(new File(this.getClass().getResource("/input.json").getPath()))
                .build();

        Select<Record> select = select()
                .from(jSql.getTable());

        Result<Record> results = jSql.execute(select);

        assertEquals(50, results.size());

        jSql.close();
    }

    /**
     * This test checks the integration of JSql with a single JSON input.
     * It creates a JSql instance, executes a query to select a record with id 1,
     * and verifies the result.
     */
    @Test
    void testIntegrationUC_004() {
        JSql jSql = JSql.create()
                .withJson(INPUT_SINGLE_JSON)
                .build();

        Select<Record> select = select()
                .from(jSql.getTable())
                .where(field("id").eq("1"));

        Result<Record> results = jSql.execute(select);

        assertEquals(1, results.size());
        Record record = results.get(0);
        assertEquals(1, record.get(DSL.field(DSL.name("ID")), Integer.class));
        assertEquals("Alice", record.get("NAME", String.class));
        assertEquals("45", record.get("AGE"));

        jSql.close();
    }

}
