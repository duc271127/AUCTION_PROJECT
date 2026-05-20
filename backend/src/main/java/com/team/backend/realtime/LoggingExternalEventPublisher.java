package com.team.backend.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple logger-based ExternalEventPublisher for development and demo.
 * Registering this bean resolves the autowire error and provides a safe default publisher.
 */
@Component
public class LoggingExternalEventPublisher implements ExternalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingExternalEventPublisher.class);

    @Override
    public void publish(String eventType, String payload) {
        // Best-effort publish for dev: log the event. Throwing is optional here.
        log.info("Publish event type={} payload={}", eventType, payload);
    }
}
