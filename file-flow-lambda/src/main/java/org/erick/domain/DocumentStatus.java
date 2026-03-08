package org.erick.domain;

public enum DocumentStatus {
    READY_FOR_UPLOAD(0, "Ready for upload"),
    UPLOADED(1, "Uploaded"),
    DONE(2, "Done"),
    FAILED(3, "Failed");

    private Integer codigo;
    private String descricao;

    DocumentStatus(Integer codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public static DocumentStatus valueOf(Integer codigo) {
        for (DocumentStatus status : DocumentStatus.values()) {
            if (status.getCodigo().equals(codigo)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid DocumentStatus codigo: " + codigo);
    }
}

