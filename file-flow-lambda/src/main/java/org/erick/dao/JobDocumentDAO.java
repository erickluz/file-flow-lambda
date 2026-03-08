package org.erick.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.erick.domain.DocumentStatus;
import org.erick.domain.JobDocument;
import org.erick.util.SQLConnection;

public class JobDocumentDAO {

    private final Connection sqlConnection;

    public JobDocumentDAO() throws SQLException {
        this.sqlConnection = new SQLConnection().getConnection();
    }

    public JobDocument buscarDocumento(Long UUIDDocument) throws SQLException {
        String sql = "SELECT * FROM job_document WHERE documentuuid = ?";

        PreparedStatement ps = sqlConnection.prepareStatement(sql);
        ps.setLong(1, UUIDDocument);

        ResultSet rs = ps.executeQuery();

        if (!rs.next()) return null;
        
        LocalDateTime lCreatedAt = null;
        Timestamp createdAt = rs.getTimestamp("CREATED_AT");
        if (createdAt != null) {
            lCreatedAt = createdAt.toLocalDateTime();
        }
        LocalDateTime lUpdatedAt = null;
        Timestamp updatedAt = rs.getTimestamp("UPDATED_AT");
        if (updatedAt != null) {
            lUpdatedAt  = updatedAt.toLocalDateTime();
        }

        return new JobDocument(
                rs.getLong("ID"),
                rs.getLong("DOCUMENTUUID"),
                DocumentStatus.valueOf(rs.getInt("STATUS")),
                rs.getString("ORIGINAL_FILENAME"),
                rs.getString("CONTENT_TYPE"),
                rs.getString("RAW_KEY"),
                rs.getString("RESULT_KEY"),
                rs.getLong("SIZE_BYTES"),
                rs.getString("E_TAG"),
                rs.getString("ERROR_MESSAGE"),
                lCreatedAt,
                lUpdatedAt
        );
    }

    public void atualizarDocumento(JobDocument jobDocumento, Long contentLength, String contetType, String eTag, long sizeBytes) {
        String sql = """
            update job_document
            set status = ?, size_bytes = ?, content_type = ?, e_tag = ?, updated_at = now()
            where id = ?
            """;

        PreparedStatement ps;
        try {
            ps = sqlConnection.prepareStatement(sql);
            ps.setInt(1, DocumentStatus.DONE.getCodigo());
            ps.setLong(2, contentLength);
            ps.setString(3, contetType);
            ps.setString(4, eTag);
            ps.setLong(5, jobDocumento.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating document: " + e.getMessage());
        }
    }

    public void atualizarDocumentoComFalha(Long id, String errorMessage) {
        String sql = """
            update job_document
            set status = ?, error_message = ?, updated_at = now()
            where id = ?
            """;

        PreparedStatement ps;
        try {
            ps = sqlConnection.prepareStatement(sql);
            ps.setInt(1, DocumentStatus.FAILED.getCodigo());
            ps.setString(2, errorMessage);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating document with failure: " + e.getMessage());
        }
    }

    public Integer contarDocumentosPorJobEStatus(Long idJob, DocumentStatus done) {
        String sql = "SELECT COUNT(*) FROM job_document WHERE job_id = ? AND status = ?";

        PreparedStatement ps;
        try {
            ps = sqlConnection.prepareStatement(sql);
            ps.setLong(1, idJob);
            ps.setInt(2, done.getCodigo());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting documents: " + e.getMessage());
        }
    }

}
