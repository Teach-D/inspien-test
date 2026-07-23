package com.inspien.eai.history;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class IntegrationHistoryRepository {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IntegrationHistoryRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public IntegrationHistoryRepository(@Qualifier("localJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String correlationId, String interfaceId, String stage, String status,
                        String sourcePayload, String targetPayload, String errorDetail,
                        Integer retrySeq, String logLevel) {
        String sql = """
                INSERT INTO INTEGRATION_HISTORY
                    (HISTORY_ID, CORRELATION_ID, INTERFACE_ID, STAGE, STATUS,
                     SOURCE_PAYLOAD, TARGET_PAYLOAD, ERROR_DETAIL, RETRY_SEQ, LOG_LEVEL, CREATED_AT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            jdbcTemplate.update(sql,
                    UUID.randomUUID().toString(), correlationId, interfaceId, stage, status,
                    sourcePayload, targetPayload, errorDetail, retrySeq, logLevel, LocalDateTime.now());
        } catch (Exception e) {
            log.error("[{}][{}] Integration History 기록 실패: {}", correlationId, interfaceId, e.getMessage(), e);
        }
    }
}
