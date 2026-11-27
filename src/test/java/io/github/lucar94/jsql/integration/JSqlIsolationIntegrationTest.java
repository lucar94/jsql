package io.github.lucar94.jsql.integration;

import io.github.lucar94.jsql.JSql;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.jooq.impl.DSL.select;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests checking that every JSql instance works on its own database.
 */
class JSqlIsolationIntegrationTest {

    @Test
    @DisplayName("Two instances open at the same time with the same table name don't interfere")
    void twoOpenInstances() {
        try (JSql first = JSql.create().withJson("[ { \"name\": \"Alice\" } ]").build();
             JSql second = JSql.create().withJson("[ { \"name\": \"Bob\" }, { \"name\": \"Carol\" } ]").build()) {

            Result<Record> firstRows = first.execute(select().from(first.getTable()));
            Result<Record> secondRows = second.execute(select().from(second.getTable()));

            assertEquals(1, firstRows.size());
            assertEquals("Alice", firstRows.get(0).get("NAME", String.class));
            assertEquals(2, secondRows.size());
        }
    }

    @Test
    @DisplayName("An instance left open does not affect instances created later")
    void instanceLeftOpen() {
        JSql leftOpen = JSql.create().withJson("[ { \"id\": 1 } ]").build();
        try {
            try (JSql later = JSql.create().withJson("[ { \"id\": 2 }, { \"id\": 3 } ]").build()) {
                assertEquals(2, later.execute(select().from(later.getTable())).size());
            }
            assertEquals(1, leftOpen.execute(select().from(leftOpen.getTable())).size());
        } finally {
            leftOpen.close();
        }
    }

    @Test
    @DisplayName("Instances built and queried in parallel threads each see only their own data")
    void parallelInstances() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 1; i <= threads; i++) {
                int rows = i;
                tasks.add(() -> {
                    StringBuilder json = new StringBuilder("[");
                    for (int r = 0; r < rows; r++) {
                        json.append(r == 0 ? "" : ",").append("{\"id\":").append(r).append('}');
                    }
                    json.append(']');
                    try (JSql jSql = JSql.create().withJson(json.toString()).build()) {
                        return jSql.execute(select().from(jSql.getTable())).size();
                    }
                });
            }

            List<Future<Integer>> results = executor.invokeAll(tasks);
            for (int i = 0; i < threads; i++) {
                assertEquals(i + 1, results.get(i).get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

}
