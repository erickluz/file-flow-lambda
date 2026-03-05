package org.erick.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.erick.domain.DocumentStatus;
import org.erick.domain.JobDocument;
import org.erick.util.SQLConnection;

public class JobDocumentDAO {

    private final Connection sqlConnection;

    public JobDocumentDAO() throws SQLException {
        this.sqlConnection = new SQLConnection().getConnection();
    }

    public JobDocument buscarDocumento(Long idDocument) throws SQLException {
        String sql = "SELECT * FROM documents WHERE document_id = ?";

        PreparedStatement ps = sqlConnection.prepareStatement(sql);
        ps.setLong(1, idDocument);

        ResultSet rs = ps.getResultSet();
        JobDocument jobDocument= null;
        while (rs.next()) {
            jobDocument = new JobDocument(
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
                rs.getTimestamp("CREATED_AT").toLocalDateTime(),
                rs.getTimestamp("UPDATED_AT").toLocalDateTime()
            );
        }
        return jobDocument;
    }

    public void atualizarDocumento(JobDocument jobDocumento, Long contentLength, String contetType, String eTag, long sizeBytes) {
        String sql = """
            update documents
            set status = ?, size_bytes = ?, content_type = ?, e_tag = ?, updated_at = now()
            where document_id = ?
            """;

        PreparedStatement ps;
        try {
            ps = sqlConnection.prepareStatement(sql);
            ps.setInt(1, DocumentStatus.UPLOADED.getCodigo());
            ps.setLong(2, contentLength);
            ps.setString(3, contetType);
            ps.setString(4, eTag);
            ps.setLong(5, jobDocumento.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating document: " + e.getMessage());
        }
    }

    public void atualizarDocumentoComFalha(Long id, String errorMessage) {
        String sql = """
            update documents
            set status = ?, error_message = ?, updated_at = now()
            where document_id = ?
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

    public void alteraStatusJob(Long idJob) throws SQLException {
        String sql = """
            update jobs
            set status = ?, updated_at = now()
            where job_id = ?
            """;

        PreparedStatement ps = sqlConnection.prepareStatement(sql);   
        ps.setInt(1, DocumentStatus.PROCESSING.getCodigo());
        ps.setLong(2, idJob);             
        
    }


}
