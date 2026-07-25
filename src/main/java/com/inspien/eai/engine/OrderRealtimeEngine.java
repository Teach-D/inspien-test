package com.inspien.eai.engine;

import com.inspien.eai.adapter.JdbcAdapter;
import com.inspien.eai.history.IntegrationHistoryRepository;
import com.inspien.eai.mapper.MappingRegistry;
import com.inspien.eai.mapper.OrderRow;
import com.inspien.eai.message.Message;
import com.inspien.eai.outbox.Outbox;
import com.inspien.eai.outbox.OutboxRelay;
import com.inspien.eai.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OrderRealtimeEngine extends InterfaceEngine {

    private static final String INTERFACE_ID = "IF-ORD-001";

    private final MappingRegistry mappingRegistry;
    private final JdbcAdapter jdbcAdapter;
    private final OutboxRepository outboxRepository;
    private final OutboxRelay outboxRelay;
    private final TransactionTemplate transactionTemplate;
    private final String applicantName;

    public OrderRealtimeEngine(IntegrationHistoryRepository history,
                                MappingRegistry mappingRegistry,
                                JdbcAdapter jdbcAdapter,
                                OutboxRepository outboxRepository,
                                OutboxRelay outboxRelay,
                                TransactionTemplate transactionTemplate,
                                @Value("${eai.applicant-name}") String applicantName) {
        super(history);
        this.mappingRegistry = mappingRegistry;
        this.jdbcAdapter = jdbcAdapter;
        this.outboxRepository = outboxRepository;
        this.outboxRelay = outboxRelay;
        this.transactionTemplate = transactionTemplate;
        this.applicantName = applicantName;
    }

    @Override
    protected void validate(Message message) {
        String xml = (String) message.getPayload();
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("주문 XML이 비어 있습니다");
        }
        if (message.getHeaders().applicantKey() == null) {
            throw new IllegalArgumentException("APPLICANT_KEY 누락");
        }
    }

    @Override
    protected Message map(Message message) {
        return mappingRegistry.get(INTERFACE_ID).map(message);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void route(Message mapped) {
        List<OrderRow> rows = (List<OrderRow>) mapped.getPayload();
        String correlationId = mapped.getHeaders().correlationId();
        String applicantKey = mapped.getHeaders().applicantKey();

        String fileName = buildFileName();

        Outbox outbox = transactionTemplate.execute(status -> {
            List<OrderRow> insertedRows = jdbcAdapter.insertOrders(rows);
            String fileContent = buildReceiptContent(insertedRows);
            String outboxId = outboxRepository.insert(applicantKey, correlationId, fileName, fileContent);
            return new Outbox(outboxId, applicantKey, correlationId, fileName, fileContent, "N", 0);
        });

        outboxRelay.sendNow(outbox, correlationId);
    }

    @Override
    protected EngineResult onError(Message message, Exception e) {
        String correlationId = message.getHeaders().correlationId();
        history.record(correlationId, INTERFACE_ID, "FAILED", "FAIL",
                null, null, summarize(e), null, "ERROR");
        return EngineResult.failure(correlationId, e.getMessage());
    }

    private String buildFileName() {
        String ts = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "INSPIEN_" + applicantName + "_" + ts + ".txt";
    }

    private String buildReceiptContent(List<OrderRow> rows) {
        StringBuilder sb = new StringBuilder();
        for (OrderRow r : rows) {
            sb.append(String.join("^",
                    r.orderId(), r.userId(), r.itemId(), r.applicantKey(),
                    r.name(), r.address(), r.itemName(), r.price()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String summarize(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
