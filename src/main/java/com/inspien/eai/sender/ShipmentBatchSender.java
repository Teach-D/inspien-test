package com.inspien.eai.sender;

import com.inspien.eai.adapter.JdbcAdapter;
import com.inspien.eai.engine.ShipmentBatchEngine;
import com.inspien.eai.mapper.OrderRow;
import com.inspien.eai.message.Message;
import com.inspien.eai.message.MessageHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShipmentBatchSender {

    private static final int CHUNK_SIZE = 200;

    private final JdbcAdapter jdbcAdapter;
    private final ShipmentBatchEngine shipmentBatchEngine;
    private final String applicantKey;

    public ShipmentBatchSender(JdbcAdapter jdbcAdapter,
                                ShipmentBatchEngine shipmentBatchEngine,
                                @Value("${eai.applicant-key}") String applicantKey) {
        this.jdbcAdapter = jdbcAdapter;
        this.shipmentBatchEngine = shipmentBatchEngine;
        this.applicantKey = applicantKey;
    }

    @Scheduled(fixedRateString = "${eai.batch.interval-ms:300000}")
    public void runShipmentTransfer() {
        List<OrderRow> pending = jdbcAdapter.findUnshippedOrders(applicantKey, CHUNK_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        MessageHeaders headers = MessageHeaders.of("IF-SHP-001", applicantKey);
        Message message = new Message(headers, pending);

        shipmentBatchEngine.process(message);
    }
}
