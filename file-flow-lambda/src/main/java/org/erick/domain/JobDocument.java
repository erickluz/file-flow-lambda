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
) {}