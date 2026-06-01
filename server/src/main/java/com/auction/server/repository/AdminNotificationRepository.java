package com.auction.server.repository;

import com.auction.server.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, UUID> {
    List<AdminNotification> findTop10ByOrderByCreatedAtDesc();
}

