package com.team.backend.service.impl;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.Item;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.AuctionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AuctionServiceImpl - implementation mở rộng cho Phase 1/Phase 2.
 * - Giữ nguyên API cũ createAuction(Auction) để tương thích.
 * - Thêm createAuction(AuctionCreateDto, UUID sellerId) để hỗ trợ tạo auction từ DTO
 *   (nếu DTO có itemId thì dùng item hiện có; nếu không có thì tạo item mới từ itemName/startPrice).
 * - Validate start/end time, ownership, và set initial currentPrice + state.
 */
@Service
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;

    public AuctionServiceImpl(AuctionRepository auctionRepository, ItemRepository itemRepository) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
    }

    // -------------------------
    // Existing API (keeps compatibility)
    // -------------------------

    @Override
    @Transactional
    public Auction createAuction(Auction auction) {
        if (auction == null) {
            throw new BusinessRuleException("Auction payload is required");
        }
        if (auction.getStartTime() == null || auction.getEndTime() == null) {
            throw new BusinessRuleException("Start time and end time are required");
        }
        if (!auction.getStartTime().isBefore(auction.getEndTime())) {
            throw new BusinessRuleException("startTime must be before endTime");
        }
        if (auction.getItem() == null) {
            throw new BusinessRuleException("Auction must reference an Item");
        }
        if (auction.getItem().getStartPrice() <= 0) {
            throw new BusinessRuleException("Item startPrice must be positive");
        }

        // set initial state and price
        Instant now = Instant.now();
        auction.setCurrentPrice(auction.getItem().getStartPrice());
        if (auction.getStartTime().isAfter(now)) {
            auction.setState(AuctionState.OPEN);
        } else if (!auction.getEndTime().isBefore(now)) {
            auction.setState(AuctionState.RUNNING);
        } else {
            throw new BusinessRuleException("endTime must be in the future");
        }

        return auctionRepository.save(auction);
    }

    @Override
    public Auction getAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
    }

    @Override
    public List<Auction> listAuctions() {
        return auctionRepository.findAll();
    }

    @Override
    @Transactional
    public Auction updateAuction(Auction auction) {
        if (auction == null || auction.getId() == null) {
            throw new BusinessRuleException("Auction and auction id are required for update");
        }
        // Optionally validate transitions here
        return auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void closeAuction(UUID auctionId) {
        Auction a = getAuction(auctionId);
        if (a.getState() == AuctionState.FINISHED || a.getState() == AuctionState.CANCELED) {
            throw new BusinessRuleException("Auction already finished or canceled");
        }
        a.setState(AuctionState.FINISHED);
        auctionRepository.save(a);
    }

    // -------------------------
    // Extended API for Phase 2 compatibility
    // -------------------------

    /**
     * Create auction from DTO. Behavior:
     * - If dto.itemId != null: load item and validate seller ownership.
     * - Else: create new Item from dto.itemName/itemDescription/startPrice and set sellerId.
     * - Validate startTime < endTime and set initial state (OPEN or RUNNING) and currentPrice.
     *
     * @param dto DTO chứa thông tin tạo auction (có thể chứa itemId hoặc itemName/startPrice)
     * @param sellerId UUID của seller (phải được cung cấp, lấy từ authenticated user)
     * @return saved Auction
     */
    @Transactional
    public Auction createAuction(AuctionCreateDto dto, UUID sellerId) {
        if (dto == null) {
            throw new BusinessRuleException("AuctionCreateDto is required");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }

        Item item;
        // If itemId provided, use existing item
        if (dto.itemId != null) {
            item = itemRepository.findById(dto.itemId)
                    .orElseThrow(() -> new BusinessRuleException("Item not found: " + dto.itemId));
            if (!sellerId.equals(item.getSellerId())) {
                throw new BusinessRuleException("Seller does not own the item");
            }
        } else {
            // create new item from provided fields
            if (dto.itemName == null || dto.itemName.trim().isEmpty()) {
                throw new BusinessRuleException("itemName is required when itemId is not provided");
            }
            if (dto.startPrice <= 0) {
                throw new BusinessRuleException("startPrice must be positive when creating new item");
            }
            Item newItem = new Item();
            newItem.setName(dto.itemName.trim());
            newItem.setDescription(dto.itemDescription == null ? "" : dto.itemDescription.trim());
            newItem.setStartPrice(dto.startPrice);
            newItem.setSellerId(sellerId);
            item = itemRepository.save(newItem);
        }

        if (dto.startTime == null || dto.endTime == null) {
            throw new BusinessRuleException("startTime and endTime are required");
        }
        if (!dto.startTime.isBefore(dto.endTime)) {
            throw new BusinessRuleException("startTime must be before endTime");
        }

        Instant now = Instant.now();
        Auction auction = new Auction();
        auction.setItem(item);
        auction.setStartTime(dto.startTime);
        auction.setEndTime(dto.endTime);
        auction.setCurrentPrice(item.getStartPrice());

        if (dto.startTime.isAfter(now)) {
            auction.setState(AuctionState.OPEN);
        } else if (!dto.endTime.isBefore(now)) {
            auction.setState(AuctionState.RUNNING);
        } else {
            throw new BusinessRuleException("endTime must be in the future");
        }

        return auctionRepository.save(auction);
    }

    /**
     * List auctions by state.
     * @param state AuctionState to filter
     * @return list of auctions in that state
     */
    public List<Auction> listAuctionsByState(AuctionState state) {
        if (state == null) {
            return listAuctions();
        }
        return auctionRepository.findByState(state);
    }

    // -------------------------
    // Helper methods (internal)
    // -------------------------

    /**
     * Validate that auction exists and is in a state that allows bidding.
     * (This helper can be used by BidService before placing a bid.)
     */
    public void validateAuctionOpenForBidding(UUID auctionId) {
        Auction a = getAuction(auctionId);
        if (a.getState() != AuctionState.OPEN && a.getState() != AuctionState.RUNNING) {
            throw new BusinessRuleException("Auction is not open for bidding");
        }
        Instant now = Instant.now();
        if (now.isBefore(a.getStartTime()) || now.isAfter(a.getEndTime())) {
            throw new BusinessRuleException("Auction is not within active time window");
        }
    }
}
