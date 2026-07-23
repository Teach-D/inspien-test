package com.inspien.eai.history;

import java.time.LocalDateTime;

public record IntegrationHistory(
        String historyId,
        String correlationId,
        String interfaceId,
        String stage,
        String status,
        String sourcePayload,
        String targetPayload,
        String errorDetail,
        Integer retrySeq,
        String logLevel,
        LocalDateTime createdAt
) {
}
