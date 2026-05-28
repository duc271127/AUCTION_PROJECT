package com.team.backend.service.impl;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDetailResponse;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.BidTransaction;
import com.team.backend.entity.Item;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.mapper.AuctionMapper;
import com.team.backend.repository.AuctionCountProjection;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.FavoriteRepository;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.AuctionHelper;
import com.team.backend.service.AuctionService;
import com.team.backend.service.EventPublisher;
import com.team.backend.service.bid.BidWalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuctionServiceImpl implements AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionServiceImpl.class);
    private static final double DEFAULT_MIN_INCREMENT = 1.0;

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final AutoBidRepository autoBidRepository;
    private final BidRepository bidRepository;
    private final FavoriteRepository favoriteRepository;
    private final AuctionHelper auctionHelper;
    private final EventPublisher eventPublisher;
    private final BidWalletService bidWalletService;

    public AuctionServiceImpl(AuctionRepository auctionRepository,
                              ItemRepository itemRepository,
                              AutoBidRepository autoBidRepository,
                              BidRepository bidRepository,
                              FavoriteRepository favoriteRepository,
                              AuctionHelper auctionHelper,
                              BidWalletService bidWalletService,
                              Optional<EventPublisher> eventPublisherOptional) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.autoBidRepository = autoBidRepository;
        this.bidRepository = bidRepository;
        this.favoriteRepository = favoriteRepository;
        this.auctionHelper = auctionHelper;
        this.bidWalletService = bidWalletService;
        this.eventPublisher = eventPublisherOptional.orElse(null);
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

        if (auction.getItemId() == null && auction.getItem() != null && auction.getItem().getId() != null) {
            auction.setItemId(auction.getItem().getId());
        }

        if (auction.getItemId() != null && auctionRepository.existsByItemId(auction.getItemId())) {
            throw new BusinessRuleException("Auction already exists for item: " + auction.getItemId());
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
        return createAuction(dto, sellerId, sellerId);
    }

    @Override
    @Transactional
    public Auction createAuction(AuctionCreateDto dto, UUID sellerId, UUID actorId) {
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
            if (auctionRepository.existsByItemId(item.getId())) {
                throw new BusinessRuleException("Auction already exists for item: " + item.getId());
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
        auction.setCurrentPrice(resolveStartingPrice(dto, item));
        auction.setReservePrice(dto.getReservePrice() == null ? item.getReservePrice() : dto.getReservePrice());
        auction.setCreatedBy(actorId == null ? sellerId : actorId);
        auction.setSellerId(sellerId);
        applyDerivedAuctionFields(auction, item, sellerId);
        return createAuction(auction);
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
        populateTransientFields(auctions);
        return auctions;
    }

    @Override
    public List<Auction> listAuctionsByState(AuctionState state) {
        if (state == null) {
            return listAuctions();
        }
        List<Auction> auctions = auctionRepository.findByState(state);
        populateTransientFields(auctions);
        return auctions;
    }

    @Override
    public List<Auction> listWonAuctions(UUID winnerId) {
        if (winnerId == null) {
            return List.of();
        }

        List<Auction> auctions = auctionRepository.findByWinnerIdOrderByEndTimeDesc(winnerId);
        populateTransientFields(auctions);
        return auctions;
    }

    @Override
    public Page<Auction> searchCatalog(String category, String q, AuctionState state, Pageable pageable) {
        Page<Auction> page = auctionRepository.searchCatalog(category, q, state, pageable);
        populateTransientFields(page.getContent());
        return page;
    }

    @Override
    public Page<Auction> searchTrendingCatalog(String category, String q, AuctionState state, Pageable pageable) {
        return rankCatalog(category, q, state, pageable, null, true);
    }

    @Override
    public Page<Auction> searchPersonalizedCatalog(UUID userId, String category, String q, AuctionState state, Pageable pageable) {
        return rankCatalog(category, q, state, pageable, userId, false);
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
        existing.setWinnerPaymentCaptured(auction.isWinnerPaymentCaptured());
        existing.setSellerId(auction.getSellerId());
        existing.setState(auction.getState());
        existing.setUpdatedAt(Instant.now());
        return auctionRepository.save(existing);
    }

    @Override
    @Transactional
    public void acceptAuction(UUID auctionId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getState() == AuctionState.ACTIVE) {
            return;
        }
        if (isTerminalState(auction.getState())) {
            throw new BusinessRuleException("Only incoming auctions can be accepted.");
        }

        Instant now = Instant.now();
        if (auction.getEndTime() != null && !now.isBefore(auction.getEndTime())) {
            throw new BusinessRuleException("Cannot accept an auction that has already ended.");
        }

        if (auction.getStartTime() == null || auction.getStartTime().isAfter(now)) {
            auction.setStartTime(now);
        }
        auction.setState(AuctionState.ACTIVE);
        auction.setUpdatedAt(now);
        auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void closeAuction(UUID auctionId) {
        closeAuction(auctionId, "Auction finished.");
    }

    @Override
    @Transactional
    public void closeAuction(UUID auctionId, String reason) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (isTerminalState(auction.getState())) {
            throw new BusinessRuleException("Auction is already closed");
        }

        closeAuctionInternal(auction, reason);
    }

    @Override
    @Transactional
    public void rejectAuction(UUID auctionId, String reason) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getState() == AuctionState.REJECTED) {
            return;
        }
        if (auction.getState() == AuctionState.DELETED || auction.getState() == AuctionState.FINISHED) {
            throw new BusinessRuleException("Auction cannot be rejected anymore.");
        }

        auction.setState(AuctionState.REJECTED);
        auction.setWinnerId(null);
        auction.setUpdatedAt(Instant.now());
        auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void deleteAuction(UUID auctionId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getState() == AuctionState.DELETED) {
            return;
        }
        if (auction.getState() == AuctionState.FINISHED) {
            throw new BusinessRuleException("Finished auctions cannot be deleted.");
        }

        auction.setState(AuctionState.DELETED);
        auction.setWinnerId(null);
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
            closeAuctionInternal(auction, "Auction finished.");
        }

        List<Auction> pendingSettlement = auctionRepository.findByStateAndWinnerPaymentCapturedFalse(AuctionState.FINISHED);
        for (Auction auction : pendingSettlement) {
            settleFinishedAuctionIfNeeded(auction, now);
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
        return getDetail(auctionId, false);
    }

    @Override
    @Transactional
    public AuctionDetailResponse getDetail(UUID auctionId, boolean incrementView) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (incrementView) {
            auction.setViewCount(Math.max(0L, auction.getViewCount()) + 1L);
            auction = auctionRepository.save(auction);
        }

        populateTransientFields(auction);

        UUID sellerId = auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy();
        Map<UUID, String> userNames = auctionHelper.lookupUserNames(java.util.Arrays.asList(auction.getLeaderId(), sellerId));
        String leaderName = userNames.get(auction.getLeaderId());
        String sellerName = userNames.get(sellerId);
        return AuctionMapper.toDetail(
                auction,
                auction.getBidCount() == null ? 0 : auction.getBidCount(),
                auction.getMinNextBid() == null ? auction.getCurrentPrice() + DEFAULT_MIN_INCREMENT : auction.getMinNextBid(),
                leaderName,
                sellerName
        );
    }

    @Override
    @Transactional
    public void incrementView(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        auction.setViewCount(Math.max(0L, auction.getViewCount()) + 1L);
        auctionRepository.save(auction);
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
        populateTransientFields(auction == null ? List.of() : List.of(auction));
    }

    private void populateTransientFields(List<Auction> auctions) {
        if (auctions == null || auctions.isEmpty()) {
            return;
        }

        List<Auction> normalizedAuctions = auctions.stream()
                .filter(Objects::nonNull)
                .map(this::safelySynchronizeAuctionOutcome)
                .toList();

        if (normalizedAuctions.isEmpty()) {
            return;
        }

        List<UUID> auctionIds = normalizedAuctions.stream()
                .map(Auction::getId)
                .filter(Objects::nonNull)
                .toList();

        List<AuctionCountProjection> bidCountRows = auctionIds.isEmpty()
                ? List.of()
                : bidRepository.countByAuctionIds(auctionIds);
        List<AuctionCountProjection> favoriteCountRows = auctionIds.isEmpty()
                ? List.of()
                : favoriteRepository.countByAuctionIds(auctionIds);

        Map<UUID, Long> bidCounts = toCountMap(bidCountRows);
        Map<UUID, Long> favoriteCounts = toCountMap(favoriteCountRows);
        Map<UUID, String> sellerNames = auctionHelper.lookupUserNames(normalizedAuctions.stream()
                .map(current -> current.getSellerId() != null ? current.getSellerId() : current.getCreatedBy())
                .toList());

        try {
            for (Auction auction : normalizedAuctions) {
                UUID auctionId = auction.getId();
                UUID sellerId = auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy();

                auction.setBidCount(Math.toIntExact(bidCounts.getOrDefault(auctionId, 0L)));
                auction.setFavoriteCount(favoriteCounts.getOrDefault(auctionId, 0L));
                auction.setMinNextBid(auction.getCurrentPrice() + DEFAULT_MIN_INCREMENT);
                auction.setSellerName(sellerNames.get(sellerId));
                auction.setTrendingScore(computeTrendingScore(auction));
            }
        } catch (Exception ex) {
            log.warn("Could not populate transient fields for auctions {}", auctionIds, ex);
        }
    }

    private Auction safelySynchronizeAuctionOutcome(Auction auction) {
        try {
            return synchronizeAuctionOutcomeIfNeeded(auction);
        } catch (Exception ex) {
            UUID auctionId = auction == null ? null : auction.getId();
            log.warn("Skipping auction outcome synchronization for auction {}", auctionId, ex);
            return auction;
        }
    }

    private Auction synchronizeAuctionOutcomeIfNeeded(Auction auction) {
        if (auction == null || auction.getState() == null) {
            return auction;
        }

        boolean changed = false;
        Instant now = Instant.now();
        boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();

        if (auction.getState() == AuctionState.SCHEDULED
                && auction.getStartTime() != null
                && !now.isBefore(auction.getStartTime())
                && (auction.getEndTime() == null || now.isBefore(auction.getEndTime()))) {
            auction.setState(AuctionState.ACTIVE);
            changed = true;
        }

        if ((auction.getLeaderId() == null || auction.getWinnerId() == null) && auction.getId() != null) {
            List<BidTransaction> latestBids = bidRepository.findByAuctionIdOrderByCreatedAtDesc(auction.getId());
            if (!latestBids.isEmpty()) {
                BidTransaction latestBid = latestBids.get(0);
                if (auction.getLeaderId() == null) {
                    auction.setLeaderId(latestBid.getBidderId());
                    changed = true;
                }
                if (auction.getState() == AuctionState.FINISHED && auction.getWinnerId() == null) {
                    auction.setWinnerId(latestBid.getBidderId());
                    changed = true;
                }
            }
        }

        if (!isTerminalState(auction.getState())
                && auction.getEndTime() != null
                && !now.isBefore(auction.getEndTime())) {
            if (!transactionActive) {
                auction.setState(AuctionState.FINISHED);
                if (auction.getWinnerId() == null) {
                    auction.setWinnerId(auction.getLeaderId());
                }
                auction.setUpdatedAt(now);
                return auction;
            }
            closeAuctionInternal(auction, "Auction finished.");
            return auction;
        }

        if (auction.getState() == AuctionState.FINISHED
                && auction.getWinnerId() == null
                && auction.getLeaderId() != null) {
            auction.setWinnerId(auction.getLeaderId());
            changed = true;
        }

        if (auction.getState() == AuctionState.FINISHED
                && !auction.isWinnerPaymentCaptured()
                && auction.getWinnerId() != null
                && auction.getCurrentPrice() > 0.0d) {
            if (!transactionActive) {
                return auction;
            }
            bidWalletService.captureWinnerPayment(auction);
            changed = true;
        }

        if (!changed) {
            return auction;
        }

        if (!transactionActive) {
            return auction;
        }

        auction.setUpdatedAt(now);
        return auctionRepository.save(auction);
    }

    private void settleFinishedAuctionIfNeeded(Auction auction, Instant now) {
        if (auction == null || auction.getState() != AuctionState.FINISHED) {
            return;
        }

        boolean changed = false;

        if (auction.getWinnerId() == null && auction.getLeaderId() != null) {
            auction.setWinnerId(auction.getLeaderId());
            changed = true;
        }

        if (!auction.isWinnerPaymentCaptured()
                && auction.getWinnerId() != null
                && auction.getCurrentPrice() > 0.0d) {
            bidWalletService.captureWinnerPayment(auction);
            changed = true;
        }

        if (!changed) {
            return;
        }

        auction.setUpdatedAt(now);
        auctionRepository.save(auction);
    }

    private double resolveStartingPrice(AuctionCreateDto dto, Item item) {
        if (dto.getStartPrice() > 0) {
            return dto.getStartPrice();
        }
        if (item.getStartingPrice() == null || item.getStartingPrice() <= 0) {
            throw new BusinessRuleException("startingPrice must be positive");
        }
        return item.getStartingPrice();
    }

    private void closeAuctionInternal(Auction auction, String reason) {
        auction.setState(AuctionState.FINISHED);
        auction.setWinnerId(auction.getLeaderId());
        bidWalletService.captureWinnerPayment(auction);
        auction.setUpdatedAt(Instant.now());
        auctionRepository.save(auction);
        registerAuctionClosedEvent(auction, reason);
    }

    private boolean isTerminalState(AuctionState state) {
        return state == AuctionState.FINISHED
                || state == AuctionState.CANCELLED
                || state == AuctionState.REJECTED
                || state == AuctionState.DELETED;
    }

    private void registerAuctionClosedEvent(Auction auction, String reason) {
        if (eventPublisher == null || auction == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        final UUID auctionId = auction.getId();
        final UUID winnerId = auction.getWinnerId();
        final double finalPrice = auction.getCurrentPrice();
        final Instant eventTimestamp = Instant.now();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    eventPublisher.publishAuctionClosed(auctionId, winnerId, finalPrice, eventTimestamp);
                } catch (Exception ex) {
                    log.warn("Failed to publish auction closed event for auction {} ({})", auctionId, reason, ex);
                }
            }
        });
    }

    private Page<Auction> rankCatalog(String category,
                                      String q,
                                      AuctionState state,
                                      Pageable pageable,
                                      UUID userId,
                                      boolean trendingOnly) {
        Set<String> preferredCategories = trendingOnly || userId == null
                ? Set.of()
                : resolvePreferredCategories(userId);
        List<Auction> filtered = auctionRepository.findAll()
                .stream()
                .filter(auction -> matchesCatalogFilter(auction, category, q, state))
                .toList();
        populateTransientFields(filtered);

        List<Auction> ranked = filtered
                .stream()
                .sorted(trendingOnly
                        ? Comparator.comparingDouble(this::computeTrendingScore).reversed()
                        : Comparator.<Auction>comparingDouble(auction -> computePersonalizedScore(auction, preferredCategories)).reversed()
                            .thenComparing(Auction::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ranked.size());
        List<Auction> content = start >= ranked.size() ? List.of() : ranked.subList(start, end);
        return new PageImpl<>(content, pageable, ranked.size());
    }

    private boolean matchesCatalogFilter(Auction auction, String category, String q, AuctionState state) {
        if (auction == null) {
            return false;
        }
        if (state != null && auction.getState() != state) {
            return false;
        }
        if (category != null && !category.isBlank()) {
            String auctionCategory = auction.getCategory() == null ? "" : auction.getCategory().trim();
            if (!auctionCategory.equalsIgnoreCase(category.trim())) {
                return false;
            }
        }
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase(Locale.ROOT);
            String haystack = ((auction.getTitle() == null ? "" : auction.getTitle()) + " " +
                    (auction.getDescription() == null ? "" : auction.getDescription())).toLowerCase(Locale.ROOT);
            if (!haystack.contains(needle)) {
                return false;
            }
        }
        return true;
    }

    private double computeTrendingScore(Auction auction) {
        if (auction == null) {
            return 0.0;
        }

        double bidScore = auction.getBidCount() == null ? 0.0 : auction.getBidCount() * 4.0;
        double favoriteScore = auction.getFavoriteCount() == null ? 0.0 : auction.getFavoriteCount() * 6.0;
        double viewScore = Math.max(0L, auction.getViewCount()) * 0.4;
        double freshnessBonus = 0.0;

        if (auction.getCreatedAt() != null) {
            long ageHours = Math.max(0L, ChronoUnit.HOURS.between(auction.getCreatedAt(), Instant.now()));
            freshnessBonus = Math.max(0.0, 48.0 - ageHours);
        }

        double endingSoonBonus = 0.0;
        if (auction.getEndTime() != null && auction.getState() == AuctionState.ACTIVE) {
            long remainingHours = Math.max(0L, ChronoUnit.HOURS.between(Instant.now(), auction.getEndTime()));
            endingSoonBonus = remainingHours <= 24 ? (24 - remainingHours) * 1.5 : 0.0;
        }

        return bidScore + favoriteScore + viewScore + freshnessBonus + endingSoonBonus;
    }

    private double computePersonalizedScore(Auction auction, Set<String> preferredCategories) {
        double baseScore = computeTrendingScore(auction);
        if (preferredCategories == null || preferredCategories.isEmpty()) {
            return baseScore;
        }
        String category = auction.getCategory();
        double categoryBoost = category != null && preferredCategories.contains(category.trim().toLowerCase(Locale.ROOT))
                ? 30.0
                : 0.0;
        return baseScore + categoryBoost;
    }

    private Set<String> resolvePreferredCategories(UUID userId) {
        Set<String> categories = new HashSet<>();

        favoriteRepository.findByUserId(userId).forEach(favorite ->
                auctionRepository.findById(favorite.getAuctionId())
                        .map(Auction::getCategory)
                        .ifPresent(category -> addCategoryPreference(categories, category)));

        bidRepository.findByBidderIdOrderByCreatedAtDesc(userId).stream()
                .limit(20)
                .forEach(bid -> auctionRepository.findById(bid.getAuctionId())
                        .map(Auction::getCategory)
                        .ifPresent(category -> addCategoryPreference(categories, category)));

        return categories;
    }

    private void addCategoryPreference(Set<String> categories, String category) {
        if (category == null || category.isBlank()) {
            return;
        }
        categories.add(category.trim().toLowerCase(Locale.ROOT));
    }

    private Map<UUID, Long> toCountMap(Collection<AuctionCountProjection> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }

        return rows.stream()
                .filter(row -> row.getAuctionId() != null)
                .collect(Collectors.toMap(AuctionCountProjection::getAuctionId, AuctionCountProjection::getCount, Long::sum));
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
