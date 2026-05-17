package com.prabhstein.pdfanalyser.document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAll(long documentId, List<ChunkEmbedding> chunks) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO document_chunks (document_id, page_number, chunk_index, content, char_count, embedding)
                VALUES (?, ?, ?, ?, ?, ?::vector)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ChunkEmbedding chunk = chunks.get(i);
                ps.setLong(1, documentId);
                ps.setInt(2, chunk.pageNumber());
                ps.setInt(3, chunk.chunkIndex());
                ps.setString(4, chunk.content());
                ps.setInt(5, chunk.content().length());
                ps.setString(6, VectorFormat.toSqlVector(chunk.embedding()));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
    }

    public List<DocumentChunk> findNearest(long documentId, List<Double> embedding, int limit) {
        return jdbcTemplate.query("""
                SELECT id, document_id, page_number, chunk_index, content, char_count,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM document_chunks
                WHERE document_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """, this::mapChunk, VectorFormat.toSqlVector(embedding), documentId, VectorFormat.toSqlVector(embedding), limit);
    }

    public int countByDocumentId(long documentId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM document_chunks WHERE document_id = ?
                """, Integer.class, documentId);
        return count == null ? 0 : count;
    }

    private DocumentChunk mapChunk(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentChunk(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getInt("page_number"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getInt("char_count"),
                rs.getDouble("similarity")
        );
    }

    public record ChunkEmbedding(int pageNumber, int chunkIndex, String content, List<Double> embedding) {
    }
}
