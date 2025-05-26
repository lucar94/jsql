package io.github.lucar94.jsql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;
import io.github.lucar94.jsql.internal.DbInitializer;
import io.github.lucar94.jsql.internal.JsonParser;
import io.github.lucar94.jsql.internal.impl.DbInitializerImpl;
import io.github.lucar94.jsql.internal.impl.JsonParserImpl;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.lucar94.jsql.internal.DbConstants.H2_PASSWORD;
import static io.github.lucar94.jsql.internal.DbConstants.H2_TABLE_NAME;
import static io.github.lucar94.jsql.internal.DbConstants.H2_URL;
import static io.github.lucar94.jsql.internal.DbConstants.H2_USER;

public class JSql implements AutoCloseable {

    private final JsonParser jsonParser;
    private final DbInitializer dbInitializer;

    private boolean isBuild = false;

    private String tableName = H2_TABLE_NAME;
    private Table<?> table;
    private File jsonFile;
    private String json;

    private Connection connection;
    private DSLContext dslContext;

    private JSql() {
        this.jsonParser = new JsonParserImpl();
        this.dbInitializer = new DbInitializerImpl();
    }

    public static JSql create() {
        return new JSql();
    }

    public JSql withJson(File file) {
        if (json != null) {
            throw new IllegalStateException("Cannot set JSON file after setting JSON string");
        }
        this.jsonFile = file;
        return this;
    }

    public JSql withJson(String json) {
        if (jsonFile != null) {
            throw new IllegalStateException("Cannot set JSON string after setting JSON file");
        }
        this.json = json;
        return this;
    }

    public JSql withTableName(String tableName) {
        Objects.requireNonNull(tableName, "Table name cannot be null");
        this.tableName = tableName.toUpperCase();
        return this;
    }

    public JSql build() {
        if(this.json == null && this.jsonFile == null) {
            throw new IllegalStateException("JSON data not provided. Use withJson() to set JSON data.");
        }

        try {
            this.connection = DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
        } catch (SQLException e) {
            throw new IllegalStateException("Error during create database connection", e);
        }

        this.dslContext = DSL.using(this.connection, SQLDialect.H2);
        this.table = DSL.table(DSL.name(tableName));

        List<Map<String, Object>> flattenJson = this.json == null ?
                this.jsonParser.flatten(this.jsonFile) :
                this.jsonParser.flatten(this.json);

        dbInitializer.initialize(flattenJson, this.tableName, this.dslContext);

        this.isBuild = true;
        return this;
    }

    public <R extends Record> Result<R> execute(Select<R> selectQuery) {
        if (!isBuild) {
            throw new IllegalStateException("JSql not built. Call build() before executing queries.");
        }

        return dslContext.fetch(selectQuery);
    }

    @Override
    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error closing database connection", e);
        }
    }

    public Table<?> getTable() {
        if(!isBuild) {
            throw new IllegalStateException("JSql not built. Call build() before accessing the table.");
        }
        return this.table;
    }

}
