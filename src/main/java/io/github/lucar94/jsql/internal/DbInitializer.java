package io.github.lucar94.jsql.internal;

import org.jooq.DSLContext;

public interface DbInitializer {

    void initialize(JsonRows rows, String tableName, DSLContext dslContext);

}
