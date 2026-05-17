package com.prabhstein.pdfanalyser.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public ChatMessage save(long documentId, String question, AnswerResult answerResult) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String citationsJson = writeCitations(answerResult.citations());
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO chat_messages (document_id, question, answer, not_answerable, citations_json)
                    VALUES (?, ?, ?, ?, ?::jsonb)
                    """, new String[]{"id"});
            ps.setLong(1, documentId);
            ps.setString(2, question);
            ps.setString(3, answerResult.answer());
            ps.setBoolean(4, answerResult.notAnswerable());
            ps.setString(5, citationsJson);
            return ps;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue());
    }

    public List<ChatMessage> findByDocumentId(long documentId) {
        return jdbcTemplate.query("""
                SELECT * FROM chat_messages
                WHERE document_id = ?
                ORDER BY created_at ASC
                """, this::mapMessage, documentId);
    }

    private ChatMessage findById(long id) {
        return jdbcTemplate.query("""
                SELECT * FROM chat_messages
                WHERE id = ?
                """, this::mapMessage, id).stream().findFirst().orElseThrow();
    }

    private ChatMessage mapMessage(ResultSet rs, int rowNum) throws SQLException {
        return new ChatMessage(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getBoolean("not_answerable"),
                readCitations(rs.getString("citations_json")),
                toOffsetDateTime(rs.getTimestamp("created_at"))
        );
    }

    private String writeCitations(List<Citation> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize citations", ex);
        }
    }

    private List<Citation> readCitations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not deserialize citations", ex);
        }
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(OffsetDateTime.now().getOffset());
    }
}
