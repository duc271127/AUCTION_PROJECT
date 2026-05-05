package com.team.backend.repository;

import com.team.backend.entity.Item;
import com.team.backend.entity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    List<Item> findByStatus(ItemStatus status);
}
