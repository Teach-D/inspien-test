package com.inspien.eai.dlq;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DlqService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DlqService.class);

    private final JdbcTemplate jdbcTemplate;

    public DlqService(@Qualifier("localJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void isolate(String correlationId, String interfaceId, String payload, String lastError, int retryCount) {
        jdbcTemplate.update(
                "INSERT INTO DLQ (DLQ_ID, CORRELATION_ID, INTERFACE_ID, PAYLOAD, LAST_ERROR, RETRY_COUNT, STATUS, CREATED_AT) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'WAIT', CURRENT_TIMESTAMP)",
                UUID.randomUUID().toString(), correlationId, interfaceId, payload, lastError, retryCount);
        log.error("[{}][{}] DLQ 격리: {}", correlationId, interfaceId, lastError);
    }

    public void reprocess(String dlqId, Runnable retryAction) {
        try {
            retryAction.run();
            jdbcTemplate.update("UPDATE DLQ SET STATUS = 'DONE' WHERE DLQ_ID = ?", dlqId);
        } catch (Exception e) {
            log.error("DLQ 수동 재처리 실패 dlqId={}: {}", dlqId, e.getMessage(), e);
            throw e;
        }
    }
}
