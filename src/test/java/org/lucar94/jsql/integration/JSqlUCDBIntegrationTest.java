package org.lucar94.jsql.integration;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Select;
import org.junit.jupiter.api.Test;
import org.lucar94.jsql.JSql;

import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * This class contains Use Case integration tests for the JSql class.
 * It checks the behavior of the JSql class with different JSON inputs and configurations.
 */
class JSqlUCDBIntegrationTest {

    private static final String JSON = """
            [
                {
                    "id": 1,
                    "name": "John",
                    "age": 30,
                    "city": "New York"
                },
                {
                    "id": 2,
                    "name": "Jane",
                    "surname": "Smith",
                    "age": 25,
                    "city": "Los Angeles",
                    "phones": [
                        "123-456-7890",
                        "987-654-3210"
                    ]
                },
                {
                    "id": 3,
                    "name": "Alice",
                    "city": "Chicago"
                }
            ]
            """;

    @Test
    void testIntegrationUCDB_001() {
        JSql jSql = JSql.create()
                .withJson(JSON)
                .build();

        Select<Record> select = select()
                .from(jSql.getTable());

        Result<Record> results = jSql.execute(select);
        assertEquals(3, results.size(), "Expected 3 rows in the result set");

        // Row 1
        Record row1 = results.stream().filter(r -> 1 == r.get("ID", Integer.class))
                .findFirst().orElse(null);
        assertNotNull(row1);

        assertEquals("John", row1.get("NAME", String.class));
        assertEquals(30, row1.get("AGE", Integer.class));
        assertEquals("New York", row1.get("CITY", String.class));

        // Row 2
        Record row2 = results.stream().filter(r -> 2 == r.get("ID", Integer.class))
                .findFirst().orElse(null);
        assertNotNull(row2);

        assertEquals("Jane", row2.get("NAME", String.class));
        assertEquals("Smith", row2.get("SURNAME", String.class));
        assertEquals(25, row2.get("AGE", Integer.class));
        assertEquals("Los Angeles", row2.get("CITY", String.class));
        assertEquals("123-456-7890", row2.get("PHONES_0", String.class));
        assertEquals("987-654-3210", row2.get("PHONES_1", String.class));

        // Row 3
        Record row3 = results.stream().filter(r -> 3 == r.get("ID", Integer.class))
                .findFirst().orElse(null);
        assertNotNull(row3);

        assertEquals("Alice", row3.get("NAME", String.class));
        assertNull(row3.get("AGE", Integer.class));
        assertEquals("Chicago", row3.get("CITY", String.class));

        jSql.close();
    }

}
