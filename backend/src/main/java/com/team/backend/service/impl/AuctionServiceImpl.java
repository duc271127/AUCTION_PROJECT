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
import com.team.backend.service.FavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
<<<<<<< Updated upstream
 * AuctionServiceImpl - full lifecycle management for auctions.
 *
 * - Keeps compatibility with createAuction(Auction).
 * - Adds createAuction(AuctionCreateDto, UUID sellerId) for DTO-based creation.
 * - Provides state transitions, scheduled refresh, and helpers for bidding validation.
=======
 * AuctionServiceImpl - hợp nhất hai phiên bản của bạn:
 * - Giữ các rule nghiệp vụ từ file cũ (kiểm tra item, start/end time, trạng thái SCHEDULED/ACTIVE)
 * - Thêm các API trả DTO (getDetail) để các service khác (ví dụ FavoriteService) gọi
 * - Có refreshStates định kỳ để cập nhật trạng thái auction theo thời gian
 *
 * Tất cả thông báo log/exception bằng tiếng Việt để dễ đọc.
>>>>>>> Stashed changes
 */
@Service
public class AuctionServiceImpl implements AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
    private final FavoriteService favoriteService;
    private final AuctionHelper auctionHelper;

    private static final double DEFAULT_MIN_INCREMENT = 1.0;

<<<<<<< Updated upstream
    public AuctionServiceImpl(AuctionRepository auctionRepository, ItemRepository itemRepository) {
=======
    public AuctionServiceImpl(AuctionRepository auctionRepository,
                              ItemRepository itemRepository,
                              BidRepository bidRepository,
                              FavoriteService favoriteService,
                              AuctionHelper auctionHelper) {
>>>>>>> Stashed changes
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.bidRepository = bidRepository;
        this.favoriteService = favoriteService;
        this.auctionHelper = auctionHelper;
    }

<<<<<<< Updated upstream
    // -------------------------
    // Existing API (keeps compatibility)
    // -------------------------
=======
    // Create / Read / Update
>>>>>>> Stashed changes

    @Override
    @Transactional
    public Auction createAuction(Auction auction) {
        if (auction == null) {
            throw new BusinessRuleException("Auction payload là bắt buộc");
        }
        if (auction.getStartTime() == null || auction.getEndTime() == null) {
            throw new BusinessRuleException("Start time và end time là bắt buộc");
        }
        if (!auction.getStartTime().isBefore(auction.getEndTime())) {
            throw new BusinessRuleException("startTime phải trước endTime");
        }
<<<<<<< Updated upstream
        if (auction.getItem() == null) {
            throw new BusinessRuleException("Auction must reference an Item");
        }
        if (auction.getItem().getStartingPrice() <= 0) {
            throw new BusinessRuleException("Item startPrice must be positive");
=======

        if (auction.getItem() == null && auction.getItemId() == null) {
            throw new BusinessRuleException("Auction phải tham chiếu tới một Item");
        }

        // Nếu item entity chưa được set nhưng có itemId, cố gắng load
        if (auction.getItem() == null && auction.getItemId() != null) {
            Item it = itemRepository.findById(auction.getItemId())
                    .orElseThrow(() -> new BusinessRuleException("Item không tồn tại: " + auction.getItemId()));
            auction.setItem(it);
        }

        if (auction.getItem() != null && auction.getItem().getStartingPrice() <= 0) {
            throw new BusinessRuleException("Giá khởi điểm của Item phải lớn hơn 0");
>>>>>>> Stashed changes
        }

        // set initial state and price
        Instant now = Instant.now();
<<<<<<< Updated upstream
        auction.setCurrentPrice(auction.getItem().getStartingPrice());
=======

        // Khởi tạo currentPrice từ item nếu chưa set
        if (auction.getCurrentPrice() == 0.0 && auction.getItem() != null) {
            auction.setCurrentPrice(auction.getItem().getStartingPrice());
        }

>>>>>>> Stashed changes
        if (auction.getStartTime().isAfter(now)) {
            auction.setState(AuctionState.SCHEDULED);
        } else if (!auction.getEndTime().isBefore(now)) {
            auction.setState(AuctionState.ACTIVE);
        } else {
            throw new BusinessRuleException("endTime phải ở tương lai");
        }

        if (auction.getId() == null) auction.setId(UUID.randomUUID());
        if (auction.getCreatedAt() == null) auction.setCreatedAt(Instant.now());
        auction.setUpdatedAt(Instant.now());

<<<<<<< Updated upstream
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
        if (a.getState() == AuctionState.FINISHED || a.getState() == AuctionState.CANCELLED) {
            throw new BusinessRuleException("Auction already finished or canceled");
        }
        a.setState(AuctionState.FINISHED);
        // set winnerId = leaderId if any
        a.setWinnerId(a.getLeaderId());
        auctionRepository.save(a);
=======
        Auction saved = auctionRepository.save(auction);
        log.info("Tạo auction: id={}, itemId={}, state={}", saved.getId(), saved.getItemId(), saved.getState());
        return saved;
>>>>>>> Stashed changes
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
    @Override
    @Transactional
    public Auction createAuction(AuctionCreateDto dto, UUID sellerId) {
        if (dto == null) {
            throw new BusinessRuleException("AuctionCreateDto là bắt buộc");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId là bắt buộc");
        }

        // Lấy thông tin item từ DTO (giả định DTO có getter)
        UUID dtoItemId = dto.getItemId();
        Item item;
<<<<<<< Updated upstream
        // If itemId provided, use existing item
        if (dto.itemId != null) {
            item = itemRepository.findById(dto.itemId)
                    .orElseThrow(() -> new BusinessRuleException("Item not found: " + dto.itemId));
=======

        if (dtoItemId != null) {
            // Nếu truyền itemId, load item và kiểm tra quyền sở hữu
            item = itemRepository.findById(dtoItemId)
                    .orElseThrow(() -> new BusinessRuleException("Item không tồn tại: " + dtoItemId));

>>>>>>> Stashed changes
            if (!sellerId.equals(item.getSellerId())) {
                throw new BusinessRuleException("Người bán không sở hữu item này");
            }
        } else {
<<<<<<< Updated upstream
            // create new item from provided fields
            if (dto.itemName == null || dto.itemName.trim().isEmpty()) {
                throw new BusinessRuleException("itemName is required when itemId is not provided");
            }
            if (dto.startPrice <= 0) {
                throw new BusinessRuleException("startPrice must be positive when creating new item");
=======
            // Nếu không truyền itemId, tạo Item mới từ thông tin DTO
            String itemName = dto.getItemName();
            String itemDescription = dto.getItemDescription();
            Double startPrice = dto.getStartPrice();

            if (itemName == null || itemName.trim().isEmpty()) {
                throw new BusinessRuleException("itemName là bắt buộc khi không truyền itemId");
            }
            if (startPrice == null || startPrice <= 0) {
                throw new BusinessRuleException("startPrice phải lớn hơn 0 khi tạo Item mới");
>>>>>>> Stashed changes
            }
            Item newItem = new Item();
<<<<<<< Updated upstream
            newItem.setName(dto.itemName.trim());
            newItem.setDescription(dto.itemDescription == null ? "" : dto.itemDescription.trim());
            newItem.setStartingPrice(dto.startPrice);
=======
            newItem.setName(itemName.trim());
            newItem.setDescription(itemDescription == null ? "" : itemDescription.trim());
            newItem.setStartingPrice(startPrice);
>>>>>>> Stashed changes
            newItem.setSellerId(sellerId);
            item = itemRepository.save(newItem);
        }

<<<<<<< Updated upstream
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
        auction.setCurrentPrice(item.getStartingPrice());

        if (dto.startTime.isAfter(now)) {
=======
        // Lấy thời gian bắt đầu/kết thúc từ DTO
        Instant startTime = dto.getStartTime();
        Instant endTime = dto.getEndTime();

        if (startTime == null || endTime == null) {
            throw new BusinessRuleException("startTime và endTime là bắt buộc");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessRuleException("startTime phải trước endTime");
        }

        Instant now = Instant.now();

        // Tạo entity Auction và gán các trường cần thiết
        Auction auction = new Auction();
        auction.setItem(item);
        auction.setItemId(item.getId()); // đảm bảo lưu itemId để truy vấn nhanh
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);
        auction.setCurrentPrice(item.getStartingPrice());
        auction.setCreatedBy(sellerId);

        if (auction.getId() == null) auction.setId(UUID.randomUUID());
        if (auction.getCreatedAt() == null) auction.setCreatedAt(Instant.now());
        auction.setUpdatedAt(Instant.now());

        if (startTime.isAfter(now)) {
>>>>>>> Stashed changes
            auction.setState(AuctionState.SCHEDULED);
        } else if (!endTime.isBefore(now)) {
            auction.setState(AuctionState.ACTIVE);
        } else {
            throw new BusinessRuleException("endTime phải ở tương lai");
        }

        Auction saved = auctionRepository.save(auction);
        return saved;
    }


    @Override
    public Auction getAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy auction: " + auctionId));
    }

    @Override
    public List<Auction> listAuctions() {
        List<Auction> all = auctionRepository.findAll();
        all.forEach(this::populateTransientFields);
        return all;
    }

    /**
     * List auctions by state.
     * @param state AuctionState to filter
     * @return list of auctions in that state
     */
    @Override
    public List<Auction> listAuctionsByState(AuctionState state) {
<<<<<<< Updated upstream
        if (state == null) {
            return listAuctions();
        }
        return auctionRepository.findByState(state);
=======
        if (state == null) return listAuctions();
        List<Auction> all = auctionRepository.findByState(state);
        all.forEach(this::populateTransientFields);
        return all;
>>>>>>> Stashed changes
    }

    // -------------------------
    // Helper methods (internal)
    // -------------------------

    /**
     * Validate that auction exists and is in a state that allows bidding.
     * (This helper can be used by BidService before placing a bid.)
     */
    @Override
<<<<<<< Updated upstream
    public void validateAuctionOpenForBidding(UUID auctionId) {
        Auction a = getAuction(auctionId);
        if (a.getState() != AuctionState.SCHEDULED && a.getState() != AuctionState.ACTIVE) {
            throw new BusinessRuleException("Auction is not open for bidding");
        }
        Instant now = Instant.now();
        if (now.isBefore(a.getStartTime()) || now.isAfter(a.getEndTime())) {
            throw new BusinessRuleException("Auction is not within active time window");
=======
    @Transactional
    public Auction updateAuction(Auction auction) {
        if (auction == null || auction.getId() == null) {
            throw new BusinessRuleException("Auction và auction id là bắt buộc");
        }
        Auction existing = auctionRepository.findById(auction.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy auction: " + auction.getId()));

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
        existing.setState(auction.getState());
        existing.setUpdatedAt(Instant.now());

        Auction saved = auctionRepository.save(existing);
        log.info("Cập nhật auction: id={}", saved.getId());
        return saved;
    }

    // Lifecycle operations

    @Override
    @Transactional
    public void closeAuction(UUID auctionId) {
        Auction a = getAuction(auctionId);
        if (a.getState() == AuctionState.FINISHED || a.getState() == AuctionState.CANCELLED) {
            throw new BusinessRuleException("Auction đã kết thúc hoặc bị hủy");
>>>>>>> Stashed changes
        }
        a.setState(AuctionState.FINISHED);
        if (a.getLeaderId() != null) a.setWinnerId(a.getLeaderId());
        a.setUpdatedAt(Instant.now());
        auctionRepository.save(a);
        log.info("Đã đóng auction: id={}, winner={}", auctionId, a.getWinnerId());
    }

    // -------------------------
    // Optional lifecycle helpers
    // -------------------------

    @Override
    @Transactional
    public void startAuction(UUID auctionId) {
        Auction a = getAuction(auctionId);
        if (a.getState() != AuctionState.SCHEDULED) {
            throw new BusinessRuleException("Auction không ở trạng thái SCHEDULED");
        }
        a.setState(AuctionState.ACTIVE);
<<<<<<< Updated upstream
        auctionRepository.save(a);
    }

    // -------------------------
    // Scheduler to refresh states
    // -------------------------
    // Requires @EnableScheduling in application main class.
    // Runs every 10 seconds by default; configurable via property auction.state.refresh.ms
    @Scheduled(fixedDelayString = "${auction.state.refresh.ms:10000}")
    public void scheduledRefreshStates() {
        try {
            refreshStates();
        } catch (Exception ex) {
            // log error in real app; avoid throwing to keep scheduler running
            System.err.println("Error refreshing auction states: " + ex.getMessage());
        }
=======
        a.setUpdatedAt(Instant.now());
        auctionRepository.save(a);
        log.info("Đã bắt đầu auction: id={}", auctionId);
>>>>>>> Stashed changes
    }

    @Override
    @Transactional
    public void refreshStates() {
        Instant now = Instant.now();

<<<<<<< Updated upstream
        // 1) OPEN -> RUNNING when startTime <= now
        List<Auction> toStart = auctionRepository.findByStateAndStartTimeBefore(AuctionState.SCHEDULED, now);
        for (Auction a : toStart) {
            a.setState(AuctionState.ACTIVE);
=======
        // Bắt đầu các auction đã đến giờ
        List<Auction> toStart = auctionRepository.findByStateAndStartTimeBefore(AuctionState.SCHEDULED, now);
        for (Auction a : toStart) {
            a.setState(AuctionState.ACTIVE);
            a.setUpdatedAt(now);
>>>>>>> Stashed changes
            auctionRepository.save(a);
            log.info("Chuyển auction sang ACTIVE: id={}", a.getId());
        }

<<<<<<< Updated upstream
        // 2) RUNNING -> FINISHED when endTime <= now
        List<Auction> toFinish = auctionRepository.findByStateAndEndTimeBefore(AuctionState.ACTIVE, now);
        for (Auction a : toFinish) {
            a.setState(AuctionState.FINISHED);
            a.setWinnerId(a.getLeaderId()); // leaderId may be null
            auctionRepository.save(a);
            // optionally: publish event/notification here
        }
    }
=======
        // Kết thúc các auction đã quá giờ
        List<Auction> toFinish = auctionRepository.findByStateAndEndTimeBefore(AuctionState.ACTIVE, now);
        for (Auction a : toFinish) {
            a.setState(AuctionState.FINISHED);
            a.setWinnerId(a.getLeaderId());
            a.setUpdatedAt(now);
            auctionRepository.save(a);
            log.info("Chuyển auction sang FINISHED: id={}, winner={}", a.getId(), a.getWinnerId());
        }
    }

    @Scheduled(fixedDelayString = "${auction.state.refresh.ms:10000}")
    public void scheduledRefreshStates() {
        try {
            refreshStates();
        } catch (Exception ex) {
            log.error("Lỗi khi refresh trạng thái auction: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void validateAuctionOpenForBidding(UUID auctionId) {
        Auction a = getAuction(auctionId);
        if (a.getState() != AuctionState.SCHEDULED && a.getState() != AuctionState.ACTIVE) {
            throw new BusinessRuleException("Auction không mở để đặt giá");
        }
        Instant now = Instant.now();
        if (now.isBefore(a.getStartTime()) || now.isAfter(a.getEndTime())) {
            throw new BusinessRuleException("Auction không trong khoảng thời gian hoạt động");
        }
    }

    // API trả DTO

    @Override
    public AuctionDetailResponse getDetail(UUID auctionId) {
        Auction a = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy auction: " + auctionId));

        int bidCount = bidRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId).size();
        double minNext = a.getCurrentPrice() + DEFAULT_MIN_INCREMENT;
        String leaderName = auctionHelper.lookupUserName(a.getLeaderId());

        AuctionDetailResponse dto = AuctionMapper.toDetail(a, bidCount, minNext, leaderName);
        return dto;
    }

    // Helpers

    private void populateTransientFields(Auction a) {
        if (a == null) return;
        try {
            int bidCount = bidRepository.findByAuctionIdOrderByCreatedAtAsc(a.getId()).size();
            a.setBidCount(bidCount);
            a.setMinNextBid(a.getCurrentPrice() + DEFAULT_MIN_INCREMENT);
            a.setSellerName(auctionHelper.lookupUserName(a.getCreatedBy()));
        } catch (Exception ex) {
            log.warn("Không thể populate transient fields cho auction {}: {}", a.getId(), ex.getMessage());
        }
    }
>>>>>>> Stashed changes
}
