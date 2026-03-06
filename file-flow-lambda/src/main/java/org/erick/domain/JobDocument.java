package org.erick.domain;

import java.time.LocalDateTime;

public record JobDocument(
     Long id,
     Long documentUUID,
     DocumentStatus status,
     String originalFilename,
     String contentType,
     String rawKey,
     String resultKey,
     Long sizeBytes,
     String eTag,
     String errorMessage,
     LocalDateTime createdAt,
     LocalDateTime updatedAt
) {
     public String toString() {
          return """
               JobDocument{
                    id=%d,
                    documentUUID=%d,
                    status=%s,
                    originalFilename=%s,
                    contentType=%s,
                    rawKey=%s,
                    resultKey=%s,
                    sizeBytes=%d,
                    eTag=%s,
                    errorMessage=%s,
                    createdAt=%s,
                    updatedAt=%s
               }
               """.formatted(
                    id, documentUUID, status, originalFilename, contentType, rawKey, resultKey, sizeBytes, eTag, errorMessage, createdAt, updatedAt
               );
     }
}