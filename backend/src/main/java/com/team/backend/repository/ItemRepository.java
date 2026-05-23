package com.team.backend.repository;

import com.team.backend.entity.Item;
import com.team.backend.entity.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    List<Item> findByStatus(ItemStatus status);

    long countBySellerId(UUID sellerId);

    long countBySellerIdAndStatus(UUID sellerId, ItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") UUID id);
}
