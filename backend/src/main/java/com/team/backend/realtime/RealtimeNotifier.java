package com.team.backend.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastToAuction(Long auctionId, RealtimeEvent event) {
        String destination = "/topic/auctions/" + auctionId;
        System.out.println("Broadcasting to " + destination + " -> " + event.getEventType());
        messagingTemplate.convertAndSend(destination, event);
    }
}