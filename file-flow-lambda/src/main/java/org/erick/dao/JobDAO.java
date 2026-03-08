package org.erick.dao;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.erick.domain.Job;
import org.erick.domain.JobStatus;
import org.erick.util.SQLConnection;

public class JobDAO {

    private Connection sqlConnection;

    public JobDAO() throws SQLException {
        this.sqlConnection = new SQLConnection().getConnection();
    }

    public Job obterJobPorId(Long idJob) throws SQLException {
        String sql = """
            SELECT * FROM job WHERE id = ?;
        """;

        PreparedStatement ps = sqlConnection.prepareStatement(sql);
        ps.setLong(1, idJob);   

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

        return new Job(
            JobStatus.valueOf(rs.getInt("STATUS")),
            rs.getInt("TOTAL_DOCUMENTS"),
            rs.getInt("DOCUMENTS_CREATED"),
            rs.getInt("DONE_DOCUMENTS"),
            rs.getInt("FAILED_DOCUMENTS"),
            lCreatedAt,
            lUpdatedAt
        );
    }

    public void atualizarStatusJob(Long idJob, JobStatus jobStatus) throws Exception {
         String sql = """
             UPDATE job
             SET status = ?, updated_at = now()
             WHERE id = ?;
             """;

            PreparedStatement ps = sqlConnection.prepareStatement(sql);
            ps.setInt(1, jobStatus.getCodigo());
            ps.setLong(2, idJob);   
    }
}
