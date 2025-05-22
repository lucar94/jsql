package org.lucar94.jsql.internal;

import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

public interface DbInitializer {

    void initialize(List<Map<String, Object>> json, String tableName, DSLContext dslContext);

}
