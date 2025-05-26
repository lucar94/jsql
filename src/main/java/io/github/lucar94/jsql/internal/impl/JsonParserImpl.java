package io.github.lucar94.jsql.internal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import io.github.lucar94.jsql.internal.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonParserImpl implements JsonParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<Map<String, Object>> flatten(String json) {
        List<Map<String, Object>> flatList = new ArrayList<>();
        JsonNode root = readJsonNode(json);

        if (root.isArray()) {
            for (JsonNode element : root) {
                String elementJson = element.toString();
                flatList.add(JsonFlattener.flattenAsMap(elementJson));
            }
        } else {
            flatList.add(JsonFlattener.flattenAsMap(json));
        }

        return flatList;
    }

    @Override
    public List<Map<String, Object>> flatten(File file) {
        try {
            return this.flatten(Files.readString(file.toPath()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JSON file: " + file.getAbsolutePath(), e);
        }
    }

    private JsonNode readJsonNode(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse JSON string", e);
        }
    }

}