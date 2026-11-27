package io.github.lucar94.jsql.internal;

import java.util.Map;
import java.util.function.Consumer;

/**
 * The rows of a JSON document, read lazily: nothing is kept in memory between two calls.
 */
@FunctionalInterface
public interface JsonRows {

    /**
     * Reads the JSON from the start and passes each flattened row to {@code action},
     * one at a time. Can be called more than once; each call reads the JSON again.
     */
    void forEach(Consumer<Map<String, Object>> action);

}
