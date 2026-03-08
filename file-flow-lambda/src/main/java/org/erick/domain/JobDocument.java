package org.erick.domain;

import java.time.LocalDateTime;

public class JobDocument {

    private Long id;
    private Long documentUUID;
    private DocumentStatus status;
    private String originalFilename;
    private String contentType;
    private String rawKey;
    private String resultKey;
    private Long sizeBytes;
    private String eTag;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public JobDocument(Long id,
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
                       LocalDateTime updatedAt) {
        this.id = id;
        this.documentUUID = documentUUID;
        this.status = status;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.rawKey = rawKey;
        this.resultKey = resultKey;
        this.sizeBytes = sizeBytes;
        this.eTag = eTag;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentUUID() {
        return documentUUID;
    }

    public void setDocumentUUID(Long documentUUID) {
        this.documentUUID = documentUUID;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRawKey() {
        return rawKey;
    }

    public void setRawKey(String rawKey) {
        this.rawKey = rawKey;
    }

    public String getResultKey() {
        return resultKey;
    }

    public void setResultKey(String resultKey) {
        this.resultKey = resultKey;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
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
