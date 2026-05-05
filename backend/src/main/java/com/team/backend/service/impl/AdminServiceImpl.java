package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.Item;
import com.team.backend.entity.ItemStatus;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;

    public AdminServiceImpl(ItemRepository itemRepository, AuctionRepository auctionRepository) {
        this.itemRepository = itemRepository;
        this.auctionRepository = auctionRepository;
    }

    /**
     * Approve an item (set status APPROVED, approvedBy, approvedAt).
     */
    @Override
    @Transactional
    public Item approveItem(UUID itemId, UUID adminId) {
        if (itemId == null) throw new BusinessRuleException("itemId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        if (item.getStatus() == ItemStatus.APPROVED) {
            return item; // idempotent
        }

        item.setStatus(ItemStatus.APPROVED);
        item.setApprovedBy(adminId);
        item.setApprovedAt(Instant.now());

        return itemRepository.save(item);
    }

    /**
     * Create an auction for an approved item.
     * Signature: (UUID itemId, Instant start, Instant end, UUID adminId, double startingPrice, Double reservePrice)
     */
    @Override
    @Transactional
    public Auction createAuctionForItem(UUID itemId,
                                        Instant start,
                                        Instant end,
                                        UUID adminId,
                                        double startingPrice,
                                        Double reservePrice) {

        if (itemId == null) throw new BusinessRuleException("itemId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");
        if (startingPrice <= 0) {
            throw new BusinessRuleException("startingPrice must be positive");
        }
        if (reservePrice != null && reservePrice < 0) {
            throw new BusinessRuleException("reservePrice must be non-negative");
        }
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BusinessRuleException("start must be before end");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        if (item.getStatus() != ItemStatus.APPROVED) {
            throw new BusinessRuleException("Item must be APPROVED before creating auction");
        }

        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setStartTime(start == null ? Instant.now() : start);
        auction.setEndTime(end == null ? Instant.now().plusSeconds(3600) : end);
        auction.setCreatedBy(adminId);
        auction.setCreatedAt(Instant.now());
        auction.setCurrentPrice(startingPrice);
        auction.setReservePrice(reservePrice == null ? 0.0 : reservePrice);

        Instant now = Instant.now();
        if (start != null && start.isAfter(now)) {
            auction.setState(AuctionState.SCHEDULED);
        } else {
            auction.setState(AuctionState.ACTIVE);
        }

        // return saved directly (no redundant local variable)
        return auctionRepository.save(auction);
    }
}
