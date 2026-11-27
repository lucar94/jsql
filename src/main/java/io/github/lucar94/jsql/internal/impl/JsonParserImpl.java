package io.github.lucar94.jsql.internal.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import io.github.lucar94.jsql.internal.JsonParser;
import io.github.lucar94.jsql.internal.JsonRows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Streams the JSON with Jackson: when the root is an array, its elements are read and
 * flattened one at a time, so the whole document is never held in memory.
 */
public class JsonParserImpl implements JsonParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String PARSE_ERROR = "Failed to parse JSON string";

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    @Override
    public JsonRows rows(String json) {
        return action -> read(() -> OBJECT_MAPPER.createParser(json), () -> json, PARSE_ERROR, action);
    }

    @Override
    public JsonRows rows(File file) {
        return action -> read(() -> OBJECT_MAPPER.createParser(file), () -> Files.readString(file.toPath()),
                "Failed to read JSON file: " + file.getAbsolutePath(), action);
    }

    /**
     * @param openParser opens a new Jackson parser positioned at the start of the JSON
     * @param readText   reads the whole JSON text, only used when the root is not an array
     *                   (the document is then a single row anyway)
     */
    private void read(IoSupplier<com.fasterxml.jackson.core.JsonParser> openParser, IoSupplier<String> readText,
                      String ioErrorMessage, Consumer<Map<String, Object>> action) {
        try (com.fasterxml.jackson.core.JsonParser parser = openParser.get()) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                JsonToken token;
                while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                    if (token == null) {
                        throw new IllegalStateException(PARSE_ERROR + ": unexpected end of input");
                    }
                    JsonNode element = parser.readValueAsTree();
                    action.accept(JsonFlattener.flattenAsMap(element.toString()));
                }
            } else {
                // Validate the whole document before flattening it as a single row
                parser.skipChildren();
                action.accept(JsonFlattener.flattenAsMap(readText.get()));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(PARSE_ERROR, e);
        } catch (IOException e) {
            throw new IllegalStateException(ioErrorMessage, e);
        }
    }

}
