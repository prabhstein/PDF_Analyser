package com.prabhstein.pdfanalyser.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DocumentRecord create(String originalFilename, String storedFilename, String contentType, long byteSize) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO documents (original_filename, stored_filename, content_type, byte_size, status)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            ps.setString(1, originalFilename);
            ps.setString(2, storedFilename);
            ps.setString(3, contentType);
            ps.setLong(4, byteSize);
            ps.setString(5, DocumentStatus.PROCESSING.name());
            return ps;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public List<DocumentRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT * FROM documents
                ORDER BY created_at DESC
                """, this::mapDocument);
    }

    public Optional<DocumentRecord> findById(long id) {
        return jdbcTemplate.query("""
                SELECT * FROM documents
                WHERE id = ?
                """, this::mapDocument, id).stream().findFirst();
    }

    public void markReady(long id, int pageCount) {
        jdbcTemplate.update("""
                UPDATE documents
                SET status = ?, page_count = ?, processed_at = now(), failure_reason = NULL
                WHERE id = ?
                """, DocumentStatus.READY.name(), pageCount, id);
    }

    public void markFailed(long id, String reason) {
        jdbcTemplate.update("""
                UPDATE documents
                SET status = ?, failure_reason = ?, processed_at = now()
                WHERE id = ?
                """, DocumentStatus.FAILED.name(), reason, id);
    }

    private DocumentRecord mapDocument(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentRecord(
                rs.getLong("id"),
                rs.getString("original_filename"),
                rs.getString("stored_filename"),
                rs.getString("content_type"),
                rs.getLong("byte_size"),
                rs.getInt("page_count"),
                DocumentStatus.valueOf(rs.getString("status")),
                rs.getString("failure_reason"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("processed_at"))
        );
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(OffsetDateTime.now().getOffset());
    }
}
