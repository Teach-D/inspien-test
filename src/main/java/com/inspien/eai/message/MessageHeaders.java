package com.inspien.eai.message;

import java.util.UUID;

public record MessageHeaders(
        String correlationId,
        String interfaceId,
        String applicantKey
) {
    public static MessageHeaders of(String interfaceId, String applicantKey) {
        return new MessageHeaders(UUID.randomUUID().toString(), interfaceId, applicantKey);
    }
}
