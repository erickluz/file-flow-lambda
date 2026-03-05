package org.erick.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SQLConnection {

    private Connection connection;

    private String host = env("DB_HOST");
    private int port = Integer.parseInt(envOr("DB_PORT", "5432"));
    private String db   = env("DB_NAME");
    private String user = env("DB_USER");
    private String pass = env("DB_PASSWORD");

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db +"?sslmode=require";
            connection = DriverManager.getConnection(url, user, pass);
        }
        return connection;
    }
    
    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing env: " + key);
        return v;
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
