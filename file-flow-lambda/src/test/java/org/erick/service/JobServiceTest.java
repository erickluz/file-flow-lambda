package org.erick.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.sql.SQLException;

import org.erick.dao.JobDocumentDAO;
import org.junit.jupiter.api.Test;

class JobServiceTest {

    @Test
    void deveAceitarKeyS3NoFormatoEsperado() throws SQLException {
        JobDocumentDAO jobDocumentDAO = mock(JobDocumentDAO.class);
        JobService jobService = new JobService(jobDocumentDAO);
        definirBucket(jobService, "bucket-teste");

        assertDoesNotThrow(() ->
            jobService.validarEventoS3("bucket-teste", "raw/1/8609310609556720506/file2")
        );
    }

    private void definirBucket(JobService jobService, String bucket) {
        try {
            Field field = JobService.class.getDeclaredField("bucket");
            field.setAccessible(true);
            field.set(jobService, bucket);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}