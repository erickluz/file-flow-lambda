package org.erick.handler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.erick.dto.S3EventEnvelope;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import domain.DocumentStatus;
import domain.JobDocument;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class SQSHandler implements RequestHandler<SQSEvent, Void> {

    private static final int TAMANHO_MAXIMO = 20 * 1024 * 1024;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String host = env("DB_HOST");
    private int port = Integer.parseInt(envOr("DB_PORT", "5432"));
    private String db   = env("DB_NAME");
    private String user = env("DB_USER");
    private String pass = env("DB_PASSWORD");
    private String region = System.getenv("AWS_REGION");
    private S3Client s3;
    private String bucket = System.getenv("S3_BUCKET");

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
                    String bucketEvent = record.s3().bucket().name();
                    String key = record.s3().object().key();

                    verificarBucket(bucketEvent);

                    this.s3 = obterS3Client();

                    Connection conn;
                    try {
                        conn = connect();

                        Long idJob = null;
                        Long idDocument = null;
                        validarKey(key, idJob, idDocument);
                        JobDocument jobDocumento = buscarDocumento(idDocument, conn);
                        verificarStatusDocumento(conn, jobDocumento);
                        HeadObjectResponse arquivoS3 = head(key);
                        verificarArquivoS3(arquivoS3, jobDocumento, conn);
                        atualizarDocumento(conn, jobDocumento, arquivoS3);

                        alteraStatusJob(conn, idJob);

                    } catch (Throwable e) {
                        logger.log("Error connecting to DB: " + e.getMessage() + "\n");
                    }
                }
            } catch (Exception e) {
                logger.log("Error parsing message body: " + e.getMessage() + "\n");
            }
        }
        return null;
    }

    private void atualizarDocumento(Connection conn, JobDocument jobDocumento, HeadObjectResponse arquivoS3) {
        String sql = """
            update documents
            set status = ?, size_bytes = ?, content_type = ?, e_tag = ?, updated_at = now()
            where document_id = ?
            """;

        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, DocumentStatus.UPLOADED.getCodigo());
            ps.setLong(2, arquivoS3.contentLength());
            ps.setString(3, arquivoS3.contentType());
            ps.setString(4, arquivoS3.eTag());
            ps.setLong(5, jobDocumento.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating document: " + e.getMessage());
        }
    }

    private void verificarArquivoS3(HeadObjectResponse arquivoS3, JobDocument jobDocumento, Connection conn) {
        try {
            if (arquivoS3 == null) {
                atualizarDocumentoComFalha(jobDocumento.id(), conn);
                throw new RuntimeException("Arquivo não encontrado no S3: ");
            }
            if (!arquivoS3.contentType().equals(jobDocumento.contentType())) {
                throw new RuntimeException("Tipo de conteúdo inválido para o arquivo no S3: ");
            }
            if (arquivoS3.contentLength() > TAMANHO_MAXIMO) {
                throw new RuntimeException("Arquivo no S3 excede o tamanho máximo permitido: ");
            }
        } catch (Exception e) {
            throw new RuntimeException("Arquivo não encontrado no S3: ");
        }
    }

    private void atualizarDocumentoComFalha(Long id, Connection conn) {
        String sql = """
            update documents
            set status = ?, error_message = ?, updated_at = now()
            where document_id = ?
            """;

        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, DocumentStatus.FAILED.getCodigo());
            ps.setString(2, "Arquivo não encontrado no S3");
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating document with failure: " + e.getMessage());
        }
    }

    private void verificarBucket(String bucketEvent) {
        if (!this.bucket.equals(bucketEvent)) {
            throw new RuntimeException("Bucket inválido: " + bucketEvent);
        }
    }

    public void putJson(String key, String json) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(json)
        );
    }

    public HeadObjectResponse head(String key) {
        return s3.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    private S3Client obterS3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .build();
    }

    private JobDocument buscarDocumento(Long idDocument, Connection conn) throws SQLException {
        String sql = "SELECT * FROM documents WHERE document_id = ?";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, idDocument);

        ResultSet rs = ps.getResultSet();
        JobDocument jobDocument= null;
        while (rs.next()) {
            jobDocument = new JobDocument(
                rs.getLong("ID"),
                rs.getLong("DOCUMENTUUID"),
                DocumentStatus.valueOf(rs.getInt("STATUS")),
                rs.getString("ORIGINAL_FILENAME"),
                rs.getString("CONTENT_TYPE"),
                rs.getString("RAW_KEY"),
                rs.getString("RESULT_KEY"),
                rs.getLong("SIZE_BYTES"),
                rs.getString("E_TAG"),
                rs.getString("ERROR_MESSAGE"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime(),
                rs.getTimestamp("UPDATED_AT").toLocalDateTime()
            );
        }
        return jobDocument;
    }

    private void verificarStatusDocumento(Connection conn, JobDocument jobDocumento) {
        if (jobDocumento == null) {
            throw new RuntimeException("Documento não encontrado");
        }
        if (jobDocumento.status() != DocumentStatus.DONE) {
            throw new RuntimeException("Documento já processado");
        }
        if (jobDocumento.status() == DocumentStatus.PROCESSING) {
            throw new RuntimeException("Documento em processamento");
        }
        if (jobDocumento.status() == DocumentStatus.FAILED) {
            throw new RuntimeException("Documento com falha no processamento");
        }
    }

    private void validarKey(String key, Long idJob, Long idDocument) {
        String [] partes = key.split("/");
        if (partes.length != 4) {
            throw new IllegalArgumentException("Formato da chave inválido: " + key);
        }
        if (partes[0].isBlank() || partes[1].isBlank() || partes[2].isBlank()) {
            throw new IllegalArgumentException("Chave inválida: " + key);
        }
        if (partes[0] != "raw") {
            throw new IllegalArgumentException("Chave inválida: " + key);
        }
        try{
            idJob = Long.parseLong(partes[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Chave inválida: " + key);
        }
        try{
            idDocument = Long.parseLong(partes[2]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Chave inválida: " + key);
        }
    }

    private void alteraStatusJob(Connection conn, Long idJob) throws SQLException {
        String sql = """
            update jobs
            set status = ?, updated_at = now()
            where job_id = ?
            """;

        PreparedStatement ps = conn.prepareStatement(sql);   
        ps.setInt(1, DocumentStatus.PROCESSING.getCodigo());
        ps.setLong(2, idJob);             
        
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
