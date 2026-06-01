package com.auction.server.repository;

import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findByState(AuctionState state);
    List<Auction> findByStateAndWinnerPaymentCapturedFalse(AuctionState state);
    List<Auction> findBySellerId(UUID sellerId);
    List<Auction> findByWinnerIdOrderByEndTimeDesc(UUID winnerId);

    // auctions that should start now or earlier but still OPEN
    @Query("select a from Auction a where a.state = :state and a.startTime <= :now")
    List<Auction> findByStateAndStartTimeBefore(AuctionState state, Instant now);

    // auctions that should finish now or earlier but still RUNNING
    @Query("select a from Auction a where a.state = :state and a.endTime <= :now")
    List<Auction> findByStateAndEndTimeBefore(AuctionState state, Instant now);

    // optional: find active auctions overlapping now
    @Query("select a from Auction a where a.startTime <= :now and a.endTime > :now")
    List<Auction> findActiveAuctions(Instant now);

    List<Auction> findByStateInAndStartTimeBefore(List<AuctionState> states, Instant now);

    Optional<Auction> findByItemId(UUID itemId);
    List<Auction> findByItemIdIn(Collection<UUID> itemIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select a from Auction a
            where (a.leaderId = :userId and a.state in :reservedStates)
               or (a.winnerId = :userId and a.state = :finishedState and a.winnerPaymentCaptured = false)
            order by a.endTime desc
            """)
    List<Auction> findOutstandingWalletDebtAuctions(@Param("userId") UUID userId,
                                                    @Param("reservedStates") Collection<AuctionState> reservedStates,
                                                    @Param("finishedState") AuctionState finishedState);

    Page<Auction> findByCategoryContainingIgnoreCaseAndTitleContainingIgnoreCase(String category, String q, Pageable pageable);

    @Query("""
            select a from Auction a
            where (:category is null or trim(:category) = '' or lower(coalesce(a.category, '')) = lower(:category))
              and (:q is null or trim(:q) = '' or lower(coalesce(a.title, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.description, '')) like lower(concat('%', :q, '%')))
              and (:state is null or a.state = :state)
            """)
    Page<Auction> searchCatalog(@Param("category") String category,
                                @Param("q") String q,
                                @Param("state") AuctionState state,
                                Pageable pageable);

    @Query("select count(a) from Auction a where a.item.sellerId = :sellerId and a.state = :state")
    long countBySellerIdAndState(@Param("sellerId") UUID sellerId, @Param("state") AuctionState state);

    boolean existsByItemId(UUID itemId);
}

