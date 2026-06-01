package com.auction.server.realtime;

/**
 * ExternalEventPublisher
 *
 * Abstraction for publishing domain events to an external system (Kafka, RabbitMQ, WebSocket, etc).
 * Implementations should throw an exception on failure so callers can decide retry behavior.
 */
public interface ExternalEventPublisher {
    /**
     * Publish an event.
     *
     * @param eventType logical event type name, e.g. "BidPlaced"
     * @param payload   JSON string payload
     * @throws Exception when publishing fails
     */
    void publish(String eventType, String payload) throws Exception;
}

