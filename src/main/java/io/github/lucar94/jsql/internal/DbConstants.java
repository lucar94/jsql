package io.github.lucar94.jsql.internal;

public class DbConstants {

    private DbConstants(){}

    /**
     * Unnamed in-memory database: H2 creates a private database for each connection,
     * dropped as soon as the connection is closed. Every JSql instance opens its own
     * connection, so instances never see each other's tables.
     */
    public static final String H2_URL = "jdbc:h2:mem:";
    public static final String H2_USER = "sa";
    public static final String H2_PASSWORD = "";
    public static final String H2_TABLE_NAME = "JSON_TABLE";

}
