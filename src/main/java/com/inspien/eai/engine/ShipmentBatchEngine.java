package com.inspien.eai.engine;

import com.inspien.eai.adapter.JdbcAdapter;
import com.inspien.eai.history.IntegrationHistoryRepository;
import com.inspien.eai.mapper.MappingRegistry;
import com.inspien.eai.mapper.ShipmentRow;
import com.inspien.eai.message.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class ShipmentBatchEngine extends InterfaceEngine {

    private static final String INTERFACE_ID = "IF-SHP-001";

    private final MappingRegistry mappingRegistry;
    private final JdbcAdapter jdbcAdapter;
    private final TransactionTemplate transactionTemplate;

    public ShipmentBatchEngine(IntegrationHistoryRepository history,
                                MappingRegistry mappingRegistry,
                                JdbcAdapter jdbcAdapter,
                                TransactionTemplate transactionTemplate) {
        super(history);
        this.mappingRegistry = mappingRegistry;
        this.jdbcAdapter = jdbcAdapter;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    protected void validate(Message message) {
        if (message.getPayload() == null) {
            throw new IllegalArgumentException("배치 대상 주문 목록이 없습니다");
        }
    }

    @Override
    protected Message map(Message message) {
        return mappingRegistry.get(INTERFACE_ID).map(message);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void route(Message mapped) {
        List<ShipmentRow> shipments = (List<ShipmentRow>) mapped.getPayload();
        String correlationId = mapped.getHeaders().correlationId();
        for (ShipmentRow s : shipments) {
            processOneShipment(s, correlationId);
        }
    }

    private void processOneShipment(ShipmentRow s, String correlationId) {
        try {
            transactionTemplate.execute(status -> {
                jdbcAdapter.insertShipment(s);
                int updated = jdbcAdapter.markOrderShipped(s.orderId(), s.applicantKey());
                if (updated == 0) {
                    throw new IllegalStateException("이미 처리되었거나 대상 주문 없음: orderId=" + s.orderId());
                }
                return null;
            });
        } catch (Exception e) {
            history.record(correlationId, INTERFACE_ID, "FAILED", "FAIL",
                    null, null, "orderId=" + s.orderId() + " " + e.getMessage(), null, "WARN");
        }
    }

    @Override
    protected EngineResult onError(Message message, Exception e) {
        String correlationId = message.getHeaders().correlationId();
        history.record(correlationId, INTERFACE_ID, "FAILED", "FAIL",
                null, null, e.getMessage(), null, "WARN");
        return EngineResult.failure(correlationId, e.getMessage());
    }
}
