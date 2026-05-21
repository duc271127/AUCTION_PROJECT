package com.team.backend.service.impl;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDetailResponse;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.Item;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.mapper.AuctionMapper;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.AuctionHelper;
import com.team.backend.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuctionServiceImpl implements AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionServiceImpl.class);
    private static final double DEFAULT_MIN_INCREMENT = 1.0;

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final AuctionHelper auctionHelper;

    public AuctionServiceImpl(AuctionRepository auctionRepository,
                              ItemRepository itemRepository,
                              BidRepository bidRepository,
                              AuctionHelper auctionHelper) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.bidRepository = bidRepository;
        this.auctionHelper = auctionHelper;
    }

    @Override
    @Transactional
    public Auction createAuction(Auction auction) {
        if (auction == null) {
            throw new BusinessRuleException("Auction payload is required");
        }
        if (auction.getStartTime() == null || auction.getEndTime() == null) {
            throw new BusinessRuleException("startTime and endTime are required");
        }
        if (!auction.getStartTime().isBefore(auction.getEndTime())) {
            throw new BusinessRuleException("startTime must be before endTime");
        }
        if (auction.getItem() == null && auction.getItemId() == null) {
            throw new BusinessRuleException("Auction must reference an item");
        }

        if (auction.getItem() == null && auction.getItemId() != null) {
            Item item = itemRepository.findById(auction.getItemId())
                    .orElseThrow(() -> new BusinessRuleException("Item not found: " + auction.getItemId()));
            auction.setItem(item);
        }

        if (auction.getItem() != null && auction.getItem().getStartingPrice() != null && auction.getCurrentPrice() == 0.0) {
            auction.setCurrentPrice(auction.getItem().getStartingPrice());
        }

        applyDerivedAuctionFields(auction, auction.getItem(), auction.getCreatedBy() != null ? auction.getCreatedBy() : auction.getSellerId());
        applyInitialState(auction);

        if (auction.getId() == null) {
            auction.setId(UUID.randomUUID());
        }
        if (auction.getCreatedAt() == null) {
            auction.setCreatedAt(Instant.now());
        }
        auction.setUpdatedAt(Instant.now());

        return auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public Auction createAuction(AuctionCreateDto dto, UUID sellerId) {
        if (dto == null) {
            throw new BusinessRuleException("AuctionCreateDto is required");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }

        Item item;
        if (dto.getItemId() != null) {
            item = itemRepository.findById(dto.getItemId())
                    .orElseThrow(() -> new BusinessRuleException("Item not found: " + dto.getItemId()));
            if (!sellerId.equals(item.getSellerId())) {
                throw new BusinessRuleException("Seller does not own this item");
            }
        } else {
            if (dto.getItemName() == null || dto.getItemName().isBlank()) {
                throw new BusinessRuleException("itemName is required when itemId is missing");
            }
            if (dto.getStartPrice() <= 0) {
                throw new BusinessRuleException("startPrice must be greater than 0");
            }

            Item newItem = new Item();
            newItem.setSellerId(sellerId);
            newItem.setName(dto.getItemName().trim());
            newItem.setDescription(dto.getItemDescription() == null ? "" : dto.getItemDescription().trim());
            newItem.setCategory(dto.getCategory());
            newItem.setImagePath(dto.getImageUrl());
            newItem.setStartingPrice(dto.getStartPrice());
            item = itemRepository.save(newItem);
        }

        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessRuleException("startTime and endTime are required");
        }
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BusinessRuleException("startTime must be before endTime");
        }

        Auction auction = new Auction();
        auction.setItem(item);
        auction.setItemId(item.getId());
        auction.setStartTime(dto.getStartTime());
        auction.setEndTime(dto.getEndTime());
        auction.setCurrentPrice(item.getStartingPrice() == null ? 0.0 : item.getStartingPrice());
        auction.setCreatedBy(sellerId);
        auction.setSellerId(sellerId);
        applyDerivedAuctionFields(auction, item, sellerId);
        applyInitialState(auction);

        if (auction.getId() == null) {
            auction.setId(UUID.randomUUID());
        }
        if (auction.getCreatedAt() == null) {
            auction.setCreatedAt(Instant.now());
        }
        auction.setUpdatedAt(Instant.now());

        return auctionRepository.save(auction);
    }

    @Override
    public Auction getAuction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        populateTransientFields(auction);
        return auction;
    }

    @Override
    public List<Auction> listAuctions() {
        List<Auction> auctions = auctionRepository.findAll();
        auctions.forEach(this::populateTransientFields);
        return auctions;
    }

    @Override
    public List<Auction> listAuctionsByState(AuctionState state) {
        if (state == null) {
            return listAuctions();
        }
        List<Auction> auctions = auctionRepository.findByState(state);
        auctions.forEach(this::populateTransientFields);
        return auctions;
    }

    @Override
    public Page<Auction> searchCatalog(String category, String q, AuctionState state, Pageable pageable) {
        Page<Auction> page = auctionRepository.searchCatalog(category, q, state, pageable);
        page.forEach(this::populateTransientFields);
        return page;
    }

    @Override
    @Transactional
    public Auction updateAuction(Auction auction) {
        if (auction == null || auction.getId() == null) {
            throw new BusinessRuleException("Auction id is required");
        }

        Auction existing = auctionRepository.findById(auction.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auction.getId()));

        existing.setTitle(auction.getTitle());
        existing.setDescription(auction.getDescription());
        existing.setImageUrl(auction.getImageUrl());
        existing.setCategory(auction.getCategory());
        existing.setStartTime(auction.getStartTime());
        existing.setEndTime(auction.getEndTime());
        existing.setReservePrice(auction.getReservePrice());
        existing.setCurrentPrice(auction.getCurrentPrice());
        existing.setLeaderId(auction.getLeaderId());
        existing.setWinnerId(auction.getWinnerId());
        existing.setSellerId(auction.getSellerId());
        existing.setState(auction.getState());
        existing.setUpdatedAt(Instant.now());
        return auctionRepository.save(existing);
    }

    @Override
    @Transactional
    public void closeAuction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELLED) {
            throw new BusinessRuleException("Auction is already closed");
        }

        auction.setState(AuctionState.FINISHED);
        if (auction.getLeaderId() != null) {
            auction.setWinnerId(auction.getLeaderId());
        }
        auction.setUpdatedAt(Instant.now());
        auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void startAuction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getState() != AuctionState.SCHEDULED) {
            throw new BusinessRuleException("Auction is not in SCHEDULED state");
        }

        auction.setState(AuctionState.ACTIVE);
        auction.setUpdatedAt(Instant.now());
        auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void refreshStates() {
        Instant now = Instant.now();

        List<Auction> toStart = auctionRepository.findByStateAndStartTimeBefore(AuctionState.SCHEDULED, now);
        for (Auction auction : toStart) {
            auction.setState(AuctionState.ACTIVE);
            auction.setUpdatedAt(now);
            auctionRepository.save(auction);
        }

        List<Auction> toFinish = auctionRepository.findByStateAndEndTimeBefore(AuctionState.ACTIVE, now);
        for (Auction auction : toFinish) {
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auction.setUpdatedAt(now);
            auctionRepository.save(auction);
        }
    }

    @Scheduled(fixedDelayString = "${auction.state.refresh.ms:10000}")
    public void scheduledRefreshStates() {
        try {
            refreshStates();
        } catch (Exception ex) {
            log.error("Failed to refresh auction states", ex);
        }
    }

    @Override
    public void validateAuctionOpenForBidding(UUID auctionId) {
        Auction auction = getAuction(auctionId);
        if (auction.getState() != AuctionState.SCHEDULED && auction.getState() != AuctionState.ACTIVE) {
            throw new BusinessRuleException("Auction is not open for bidding");
        }

        Instant now = Instant.now();
        if (auction.getStartTime() == null || auction.getEndTime() == null) {
            throw new BusinessRuleException("Auction schedule is incomplete");
        }
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
            throw new BusinessRuleException("Auction is outside its active time window");
        }
    }

    @Override
    public AuctionDetailResponse getDetail(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        populateTransientFields(auction);

        String leaderName = auctionHelper.lookupUserName(auction.getLeaderId());
        String sellerName = auctionHelper.lookupUserName(auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy());
        return AuctionMapper.toDetail(
                auction,
                auction.getBidCount() == null ? 0 : auction.getBidCount(),
                auction.getMinNextBid() == null ? auction.getCurrentPrice() + DEFAULT_MIN_INCREMENT : auction.getMinNextBid(),
                leaderName,
                sellerName
        );
    }

    private void applyInitialState(Auction auction) {
        Instant now = Instant.now();
        if (auction.getStartTime().isAfter(now)) {
            auction.setState(AuctionState.SCHEDULED);
        } else if (!auction.getEndTime().isBefore(now)) {
            auction.setState(AuctionState.ACTIVE);
        } else {
            throw new BusinessRuleException("endTime must be in the future");
        }
    }

    private void applyDerivedAuctionFields(Auction auction, Item item, UUID sellerId) {
        if (auction == null || item == null) {
            return;
        }

        if (auction.getSellerId() == null) {
            auction.setSellerId(sellerId != null ? sellerId : item.getSellerId());
        }
        if (auction.getCreatedBy() == null) {
            auction.setCreatedBy(auction.getSellerId());
        }
        if (isBlank(auction.getTitle())) {
            auction.setTitle(firstNonBlank(item.getName()));
        }
        if (isBlank(auction.getDescription())) {
            auction.setDescription(firstNonBlank(item.getDescription()));
        }
        if (isBlank(auction.getCategory())) {
            auction.setCategory(firstNonBlank(item.getCategory()));
        }
        if (isBlank(auction.getImageUrl())) {
            auction.setImageUrl(firstNonBlank(item.getImagePath(), firstImage(item)));
        }
    }

    private void populateTransientFields(Auction auction) {
        if (auction == null) {
            return;
        }

        try {
            int bidCount = bidRepository.findByAuctionIdOrderByCreatedAtAsc(auction.getId()).size();
            auction.setBidCount(bidCount);
            auction.setMinNextBid(auction.getCurrentPrice() + DEFAULT_MIN_INCREMENT);
            auction.setSellerName(auctionHelper.lookupUserName(auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy()));
        } catch (Exception ex) {
            log.warn("Could not populate transient fields for auction {}", auction.getId(), ex);
        }
    }

    private String firstImage(Item item) {
        if (item == null || item.getImageUrls() == null || item.getImageUrls().isEmpty()) {
            return null;
        }
        return item.getImageUrls().get(0);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
