package com.inspien.eai.engine;

import com.inspien.eai.history.IntegrationHistoryRepository;
import com.inspien.eai.message.Message;

public abstract class InterfaceEngine {

    protected final IntegrationHistoryRepository history;

    protected InterfaceEngine(IntegrationHistoryRepository history) {
        this.history = history;
    }

    public final EngineResult process(Message message) {
        String correlationId = message.getHeaders().correlationId();
        String interfaceId = message.getHeaders().interfaceId();

        history.record(correlationId, interfaceId, "RECEIVED", "SUCCESS",
                stringify(message.getPayload()), null, null, null, "INFO");

        try {
            validate(message);

            Message mapped = map(message);
            history.record(correlationId, interfaceId, "MAPPED", "SUCCESS",
                    null, stringify(mapped.getPayload()), null, null, "INFO");

            route(mapped);

            history.record(correlationId, interfaceId, "COMPLETED", "SUCCESS",
                    null, null, null, null, "INFO");
            return EngineResult.success(correlationId);

        } catch (Exception e) {
            return onError(message, e);
        }
    }

    protected abstract void validate(Message message);

    protected abstract Message map(Message message);

    protected abstract void route(Message mapped);

    protected abstract EngineResult onError(Message message, Exception e);

    private String stringify(Object payload) {
        return payload == null ? null : String.valueOf(payload);
    }
}
