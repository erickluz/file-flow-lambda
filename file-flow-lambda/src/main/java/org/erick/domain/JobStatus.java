package org.erick.domain;

public enum JobStatus {
    CREATED (0, "Job created"),
    COLLECTING (1, "Job collecting documents"),
    READ_FOR_UPLOAD (2, "Job ready for upload"),
    DONE (3, "Job done"),
    FAILED (4, "Job failed");

    private Integer codigo;
    private String description;

    private JobStatus(Integer codigo, String description) {
        this.codigo = codigo;
        this.description = description;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getDescription() {
        return description;
    }

    public static JobStatus valueOf(Integer codigo) {
        for (JobStatus status : JobStatus.values()) {
            if (status.getCodigo().equals(codigo)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid JobStatus codigo: " + codigo);
    }

}

