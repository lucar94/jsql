package io.github.lucar94.jsql.internal;

import java.io.File;

public interface JsonParser {

    JsonRows rows(String json);

    JsonRows rows(File file);

}
