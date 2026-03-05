package org.erick.handler;

import org.erick.service.JobService;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.sql.SQLException;

import org.erick.domain.JobDocument;


public class SQSHandler implements RequestHandler<SQSEvent, Void> {

    private JobService jobService;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        JobDocument jobDocumento = null;
        try {
            jobService = new JobService();
            for(var s3Envelope : jobService.obterEventos(event, logger)) {
                for (var record : s3Envelope.Records()) {
                    String bucketName = record.s3().bucket().name();
                    String key = record.s3().object().key();

                    HeadObjectResponse arquivoS3 = jobService.obterArquivoS3(key);

                    jobService.validarEventoS3(bucketName, key);
                    Long idJob = jobService.extrairIdJob(key);
                    Long idDocument = jobService.extrairIdDocument(key);

                    jobDocumento = jobService.buscarDocumento(idDocument);
                    jobService.verificarStatusDocumento(jobDocumento);
                    jobService.verificarArquivoS3(arquivoS3, jobDocumento);
                    jobService.criarArquivoResultadoS3(key, jobDocumento);
                    jobService.atualizarDocumento(jobDocumento, 
                                                  arquivoS3.contentLength(), 
                                                  arquivoS3.contentType(), 
                                                  arquivoS3.eTag(), 
                                                  arquivoS3.contentLength());
                    jobService.alteraStatusJob(idJob);
                }
            }
        } catch (SQLException e) {
            logger.log("Erro de SQL: " + e.getMessage() + "\n");
        } catch (Throwable e) {
            jobService.atualizarDocumentoComFalha(jobDocumento, e.getMessage());
            logger.log("Erro ao processar lambda: " + e.getMessage() + "\n");
        }
        
        return null;
    }

}
