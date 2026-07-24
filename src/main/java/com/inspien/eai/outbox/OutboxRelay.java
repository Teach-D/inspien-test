package com.inspien.eai.outbox;

import com.inspien.eai.factory.TransferClientFactory;
import com.inspien.eai.history.IntegrationHistoryRepository;
import com.inspien.eai.transfer.FileTransferClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelay {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxRelay.class);
    private static final int MAX_RETRY = 3;
    private static final String INTERFACE_ID_ACC = "IF-ACC-001";

    private final OutboxRepository outboxRepository;
    private final TransferClientFactory transferClientFactory;
    private final IntegrationHistoryRepository historyRepository;

    public OutboxRelay(OutboxRepository outboxRepository,
                        TransferClientFactory transferClientFactory,
                        IntegrationHistoryRepository historyRepository) {
        this.outboxRepository = outboxRepository;
        this.transferClientFactory = transferClientFactory;
        this.historyRepository = historyRepository;
    }

    public void sendNow(Outbox outbox, String correlationId) {
        attemptSend(outbox, correlationId, 0);
    }

    @Scheduled(fixedDelayString = "${eai.outbox.retry-interval-ms:60000}")
    public void retryPending() {
        outboxRepository.findPendingRetryable(MAX_RETRY)
                .forEach(o -> attemptSend(o, o.correlationId(), o.retryCount()));
    }

    private void attemptSend(Outbox outbox, String correlationId, int attemptSeq) {
        FileTransferClient client = transferClientFactory.create("FTP");
        try {
            client.upload(outbox.fileName(), outbox.payload());
            outboxRepository.markSent(outbox.outboxId());
            historyRepository.record(correlationId, INTERFACE_ID_ACC, "FTP_DONE", "SUCCESS",
                    null, outbox.fileName(), null, attemptSeq, "INFO");
            log.info("[{}][{}][FTP_DONE] ok fileName={}", correlationId, INTERFACE_ID_ACC, outbox.fileName());
        } catch (Exception e) {
            int nextRetryCount = outbox.retryCount() + 1;
            outboxRepository.incrementRetry(outbox.outboxId());

            if (nextRetryCount >= MAX_RETRY) {
                historyRepository.record(correlationId, INTERFACE_ID_ACC, "FAILED", "FAIL",
                        null, outbox.fileName(), summarize(e), nextRetryCount, "ERROR");
                log.error("[{}][{}][FAILED seq={}] 최대 재시도 초과, 포기: {}",
                        correlationId, INTERFACE_ID_ACC, nextRetryCount, e.getMessage());
            } else {
                historyRepository.record(correlationId, INTERFACE_ID_ACC, "FTP_RETRY", "FAIL",
                        null, outbox.fileName(), summarize(e), nextRetryCount, "WARN");
                log.warn("[{}][{}][FTP_RETRY seq={}] {}", correlationId, INTERFACE_ID_ACC, nextRetryCount, e.getMessage());
            }
        }
    }

    private String summarize(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
