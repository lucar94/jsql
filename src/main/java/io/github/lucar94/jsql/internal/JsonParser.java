package io.github.lucar94.jsql.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface JsonParser {

    List<Map<String, Object>> flatten(String json);

    List<Map<String, Object>> flatten(File file);

}

