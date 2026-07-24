package com.inspien.eai.outbox;

public record Outbox(
        String outboxId,
        String applicantKey,
        String correlationId,
        String fileName,
        String payload,
        String sendStatus,
        int retryCount
) {
}
