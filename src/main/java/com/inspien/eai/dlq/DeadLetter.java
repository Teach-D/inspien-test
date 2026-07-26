package com.inspien.eai.dlq;

public record DeadLetter(
        String dlqId,
        String correlationId,
        String interfaceId,
        String payload,
        String lastError,
        int retryCount,
        String status
) {
}
