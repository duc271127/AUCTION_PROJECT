package com.team.backend.service.impl;

import com.team.backend.entity.OutboxEvent;
import com.team.backend.repository.OutboxRepository;
import com.team.backend.realtime.ExternalEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxRepository outboxRepository;
    private final ExternalEventPublisher externalEventPublisher;

    // batch size to limit number of events processed per run
    private static final int BATCH_SIZE = 50;

    public OutboxDispatcher(OutboxRepository outboxRepository,
                            ExternalEventPublisher externalEventPublisher) {
        this.outboxRepository = outboxRepository;
        this.externalEventPublisher = externalEventPublisher;
    }

    /**
     * Dispatch pending outbox events every second (adjust as needed).
     * This method is transactional: read pending events, publish, mark dispatched and save.
     */
    @Scheduled(fixedDelayString = "${outbox.dispatch.delay-ms:1000}")
    public void scheduledDispatch() {
        try {
            dispatchPendingBatch();
        } catch (Exception ex) {
            log.error("OutboxDispatcher scheduled run failed: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    protected void dispatchPendingBatch() {
        List<OutboxEvent> pending = outboxRepository.findByDispatchedFalseOrderByCreatedAtAsc();
        if (pending == null || pending.isEmpty()) return;

        // limit batch
        int limit = Math.min(BATCH_SIZE, pending.size());
        for (int i = 0; i < limit; i++) {
            OutboxEvent e = pending.get(i);
            try {
                externalEventPublisher.publish(e.getEventType(), e.getPayload());
                e.setDispatched(true);
                outboxRepository.save(e); // persist dispatched flag
                log.debug("Dispatched outbox event id={}", e.getId());
            } catch (Exception ex) {
                // publish failed: do not mark dispatched; log and continue to next event
                log.warn("Failed to dispatch outbox event id={}, type={}, error={}", e.getId(), e.getEventType(), ex.getMessage());
                // optionally: increment a retry counter column and persist it (not shown)
            }
        }
    }
}
