package com.team.backend.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastToAuction(UUID auctionId, RealtimeEvent event) {
        String destination = "/topic/auctions/" + auctionId;
        messagingTemplate.convertAndSend(destination, event);
    }
}