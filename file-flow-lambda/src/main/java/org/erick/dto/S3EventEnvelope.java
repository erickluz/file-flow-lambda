package org.erick.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record S3EventEnvelope(java.util.List<S3Record> Records) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Record(S3Entity s3) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Entity(S3Bucket bucket, S3Object object) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Bucket(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Object(String key, Long size) {}
}
