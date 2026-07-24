package com.inspien.eai.outbox;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public OutboxRepository(@Qualifier("localJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String insert(String applicantKey, String correlationId, String fileName, String payload) {
        String outboxId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO OUTBOX (OUTBOX_ID, APPLICANT_KEY, CORRELATION_ID, FILE_NAME, PAYLOAD, SEND_STATUS, RETRY_COUNT, CREATED_AT) " +
                        "VALUES (?, ?, ?, ?, ?, 'N', 0, CURRENT_TIMESTAMP)",
                outboxId, applicantKey, correlationId, fileName, payload);
        return outboxId;
    }

    public void markSent(String outboxId) {
        jdbcTemplate.update("UPDATE OUTBOX SET SEND_STATUS = 'Y' WHERE OUTBOX_ID = ?", outboxId);
    }

    public void incrementRetry(String outboxId) {
        jdbcTemplate.update("UPDATE OUTBOX SET RETRY_COUNT = RETRY_COUNT + 1 WHERE OUTBOX_ID = ?", outboxId);
    }

    public List<Outbox> findPendingRetryable(int maxRetry) {
        return jdbcTemplate.query(
                "SELECT * FROM OUTBOX WHERE SEND_STATUS = 'N' AND RETRY_COUNT < ?",
                rowMapper(), maxRetry);
    }

    private RowMapper<Outbox> rowMapper() {
        return (rs, rowNum) -> new Outbox(
                rs.getString("OUTBOX_ID"),
                rs.getString("APPLICANT_KEY"),
                rs.getString("CORRELATION_ID"),
                rs.getString("FILE_NAME"),
                rs.getString("PAYLOAD"),
                rs.getString("SEND_STATUS"),
                rs.getInt("RETRY_COUNT")
        );
    }
}
