package org.erick;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dto.S3EventEnvelope;

public class SQSHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String host = env("DB_HOST");
    private int port = Integer.parseInt(envOr("DB_PORT", "5432"));
    private String db   = env("DB_NAME");
    private String user = env("DB_USER");
    private String pass = env("DB_PASSWORD");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            logger.log("Received message: " + msg.getBody());
            String body = msg.getBody();
            S3EventEnvelope s3 = null;
            try {
                s3 = MAPPER.readValue(body, S3EventEnvelope.class);
                for (var record : s3.Records()) {
                    String bucket = record.s3().bucket().name();
                    String key = record.s3().object().key();

                    Connection conn;
                    try {
                        conn = connect();

                        Integer idJob = null;
                        Integer idDocument = null;
                        validaKey(key, idJob, idDocument);
                        alteraStatusJob(conn);

                    } catch (SQLException e) {
                        logger.log("Error connecting to DB: " + e.getMessage() + "\n");
                    }
                }
            } catch (Exception e) {
                logger.log("Error parsing message body: " + e.getMessage() + "\n");
            }
        }
        return null;
    }

    private void validaKey(String key, Integer idJob, Integer idDocument) {
        String [] partes = key.split("/");
        if (partes.length != 4) {
            throw new IllegalArgumentException("Invalid key format: " + key);
        }
        if (partes[0].isBlank() || partes[1].isBlank() || partes[2].isBlank()) {
            throw new IllegalArgumentException("Key parts cannot be blank: " + key);
        }
        if (partes[0] != "raw") {
            throw new IllegalArgumentException("Key must start with 'raw': " + key);
        }
        try{
            idJob = Integer.parseInt(partes[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Third part of key must be an integer: " + key);
        }
        try{
            idDocument = Integer.parseInt(partes[2]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fourth part of key must be an integer: " + key);
        }
    }

    private void alteraStatusJob(Connection conn) {
        String sql = """
            update jobs
            set status = ?, updated_at = now()
            where job_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private Connection connect() throws SQLException {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + db +"?sslmode=require";
            return DriverManager.getConnection(url, user, pass);
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
