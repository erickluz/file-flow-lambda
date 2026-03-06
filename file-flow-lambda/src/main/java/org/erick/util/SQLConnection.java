package org.erick.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SQLConnection {

    private Connection connection;

    private String host = envOr("DB_HOST", "localhost");
    private int port = Integer.parseInt(envOr("DB_PORT", "5432"));
    private String db   = envOr("DB_NAME", "fileflow");
    private String user = envOr("DB_USER", "postgres");
    private String pass = envOr("DB_PASSWORD", "postgres");

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db +"?sslmode=require";
            connection = DriverManager.getConnection(url, user, pass);
        }
        return connection;
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
