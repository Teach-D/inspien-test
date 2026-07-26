package com.inspien.eai.sender;

import com.inspien.eai.adapter.JdbcAdapter;
import com.inspien.eai.engine.ShipmentBatchEngine;
import com.inspien.eai.history.IntegrationHistoryRepository;
import com.inspien.eai.mapper.OrderRow;
import com.inspien.eai.message.Message;
import com.inspien.eai.message.MessageHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ShipmentBatchSender {

    private static final int CHUNK_SIZE = 200;
    private static final String INTERFACE_ID = "IF-SHP-001";

    private final JdbcAdapter jdbcAdapter;
    private final ShipmentBatchEngine shipmentBatchEngine;
    private final IntegrationHistoryRepository historyRepository;
    private final String applicantKey;

    public ShipmentBatchSender(JdbcAdapter jdbcAdapter,
                                ShipmentBatchEngine shipmentBatchEngine,
                                IntegrationHistoryRepository historyRepository,
                                @Value("${eai.applicant-key}") String applicantKey) {
        this.jdbcAdapter = jdbcAdapter;
        this.shipmentBatchEngine = shipmentBatchEngine;
        this.historyRepository = historyRepository;
        this.applicantKey = applicantKey;
    }

    @Scheduled(fixedRateString = "${eai.batch.interval-ms:300000}")
    public void runShipmentTransfer() {
        String correlationId = UUID.randomUUID().toString();
        List<OrderRow> pending;
        try {
            pending = jdbcAdapter.findUnshippedOrders(applicantKey, CHUNK_SIZE);
        } catch (Exception e) {
            historyRepository.record(correlationId, INTERFACE_ID, "FAILED", "FAIL",
                    null, null, "배치 대상 조회 실패: " + e.getMessage(), null, "ERROR");
            return;
        }

        if (pending.isEmpty()) {
            return;
        }

        MessageHeaders headers = new MessageHeaders(correlationId, INTERFACE_ID, applicantKey);
        Message message = new Message(headers, pending);

        shipmentBatchEngine.process(message);
    }
}
