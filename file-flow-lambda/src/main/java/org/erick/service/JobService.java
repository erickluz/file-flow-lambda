package org.erick.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.erick.dao.JobDocumentDAO;
import org.erick.domain.DocumentStatus;
import org.erick.domain.JobDocument;
import org.erick.dto.S3EventEnvelope;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class JobService {

    private static final int TAMANHO_MAXIMO = 20 * 1024 * 1024;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String region = System.getenv("AWS_REGION");
    private S3Client s3 = obterS3Client();
    private String bucket = System.getenv("S3_BUCKET");

    private JobDocumentDAO jobDocumentDAO;

    public JobService() throws SQLException {
        this.jobDocumentDAO = new JobDocumentDAO();
    }

    public void verificarArquivoS3(HeadObjectResponse arquivoS3, JobDocument jobDocumento) {
        if (arquivoS3 == null) {
            throw new RuntimeException("Arquivo não encontrado no S3: ");
        }
        if (!arquivoS3.contentType().equals(jobDocumento.contentType())) {
            throw new RuntimeException("Tipo de conteúdo inválido para o arquivo no S3: ");
        }
        if (arquivoS3.contentLength() > TAMANHO_MAXIMO) {
            throw new RuntimeException("Arquivo no S3 excede o tamanho máximo permitido: ");
        }
    }

    public void verificarStatusDocumento(JobDocument jobDocumento) {
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

    public void atualizarDocumentoComFalha(JobDocument jobDocumento, String errorMessage) {
        if (jobDocumento != null) {
            jobDocumentDAO.atualizarDocumentoComFalha(jobDocumento.id(), errorMessage);
        }
    }

    public HeadObjectResponse obterArquivoS3(String key) {
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

    public void validarEventoS3(String bucketEvent, String key) {
        verificarBucket(bucketEvent);
        validarKey(key);
    }

    private void verificarBucket(String bucketEvent) {
        if (!this.bucket.equals(bucketEvent)) {
            throw new RuntimeException("Bucket inválido: " + bucketEvent);
        }
    }

    private void validarKey(String key) {
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
            Long.parseLong(partes[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Chave inválida: " + key);
        }
        try{
            Long.parseLong(partes[2]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Chave inválida: " + key);
        }
    }

    public List<S3EventEnvelope> obterEventos(SQSEvent event, LambdaLogger logger) {
        List<S3EventEnvelope> eventos = event.getRecords().stream()
            .map(SQSEvent.SQSMessage::getBody)
            .map(body -> {
                try {
                    return MAPPER.readValue(body, S3EventEnvelope.class);
                } catch (Exception e) {
                    logger.log("Error parsing message body: " + e.getMessage() + "\n");
                    return null;
                }
            })
            .filter(envelope -> envelope != null)
            .toList();
        if (eventos.isEmpty()) {
            throw new RuntimeException("Nenhum evento válido encontrado no SQS");
        }
        return eventos;
    }

    public Long extrairIdJob(String key) {
        String [] partes = key.split("/");
        return Long.parseLong(partes[1]);
    }

	public Long extrairIdDocument(String key) {
		String [] partes = key.split("/");
		return Long.parseLong(partes[2]);
	}
	
    public JobDocument buscarDocumento(Long idDocument) throws SQLException {
        JobDocument jobDocument = jobDocumentDAO.buscarDocumento(idDocument);
        if (jobDocument == null) {
            throw new RuntimeException("Documento não encontrado: " + idDocument);
        }
        return jobDocument;
    }

    public void atualizarDocumento(JobDocument jobDocumento, Long contentLength, String contentType, String eTag, long sizeBytes) {
        jobDocumentDAO.atualizarDocumento(jobDocumento, contentLength, contentType, eTag, sizeBytes);
    }

    public void alteraStatusJob(Long idJob) {
        try {
            jobDocumentDAO.alteraStatusJob(idJob);
        } catch (Exception e) {
            throw new RuntimeException("Error updating job status: " + e.getMessage());
        }
    }

    public void criarArquivoResultadoS3(Long jobId, String bucket, JobDocument jobDocumento) {
        putJson("processed/" + jobId + "/" + jobDocumento.id() + "result.json", arquivoResultadoJson(jobId, bucket, jobDocumento));
    }

    private String arquivoResultadoJson(Long jobId, String bucket, JobDocument jobDocumento) {
        return """
            {
                "jobId": %d,
                "documentId": %d,
                "raw":{
                    "bucket":"%s",
                    "key":"%s",
                    "originalFilename":"%s",
                    "contentType":"%s",
                    "sizeBytes": %d,
                    "etag":"%s",
                    "uploadedAt":"%s"
                },
                "processing":{
                    "processor":"lambda-docproc-v1",
                    "processedAt":"%s",
                    "status":"%s"
                }
            }
            """.formatted(
            jobId,
            jobDocumento.id(),
            bucket,
            jobDocumento.rawKey(),
            jobDocumento.originalFilename(),
            jobDocumento.contentType(),
            jobDocumento.sizeBytes(),
            jobDocumento.eTag(),
            jobDocumento.updatedAt().format(DateTimeFormatter.ISO_DATE_TIME),
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            "DONE"
        );
    }

    private void putJson(String key, String json) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(json)
        );
    }

}
