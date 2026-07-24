package com.inspien.eai.engine;

public record EngineResult(boolean success, String correlationId, String message) {

    public static EngineResult success(String correlationId) {
        return new EngineResult(true, correlationId, "OK");
    }

    public static EngineResult failure(String correlationId, String message) {
        return new EngineResult(false, correlationId, message);
    }
}
