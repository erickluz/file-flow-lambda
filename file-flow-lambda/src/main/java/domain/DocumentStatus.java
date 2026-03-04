package domain;

public enum DocumentStatus {
    READY_FOR_UPLOAD(1, "Ready for upload"),
    UPLOADED(2, "Uploaded"),
    PROCESSING(3, "Processing"),
    DONE(4, "Done"),
    FAILED(5, "Failed");

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

