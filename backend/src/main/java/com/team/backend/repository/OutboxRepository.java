package com.team.backend.repository;

import com.team.backend.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByDispatchedFalseOrderByCreatedAtAsc();
}
