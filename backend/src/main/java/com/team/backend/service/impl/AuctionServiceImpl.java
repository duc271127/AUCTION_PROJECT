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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;

    public AuctionServiceImpl(AuctionRepository auctionRepository,
                              ItemRepository itemRepository) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
    }

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

        if (auction.getItem().getStartingPrice() <= 0) {
            throw new BusinessRuleException("Item startPrice must be positive");
        }

        Instant now = Instant.now();

        auction.setCurrentPrice(auction.getItem().getStartingPrice());

        if (auction.getStartTime().isAfter(now)) {
            auction.setState(AuctionState.SCHEDULED);
        } else if (!auction.getEndTime().isBefore(now)) {
            auction.setState(AuctionState.ACTIVE);
        } else {
            throw new BusinessRuleException("endTime must be in the future");
        }

        return auctionRepository.save(auction);
    }

    @Override
    public Auction getAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Auction not found: " + auctionId));
    }

    @Override
    public List<Auction> listAuctions() {
        return auctionRepository.findAll();
    }

    @Override
    @Transactional
    public Auction updateAuction(Auction auction) {

        if (auction == null || auction.getId() == null) {
            throw new BusinessRuleException("Auction and auction id are required");
        }

        return auctionRepository.save(auction);
    }

    @Override
    @Transactional
    public void closeAuction(UUID auctionId) {

        Auction a = getAuction(auctionId);

        if (a.getState() == AuctionState.FINISHED
                || a.getState() == AuctionState.CANCELLED) {

            throw new BusinessRuleException("Auction already finished or canceled");
        }

        a.setState(AuctionState.FINISHED);
        a.setWinnerId(a.getLeaderId());

        auctionRepository.save(a);
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

        if (dto.itemId != null) {

            item = itemRepository.findById(dto.itemId)
                    .orElseThrow(() ->
                            new BusinessRuleException("Item not found: " + dto.itemId));

            if (!sellerId.equals(item.getSellerId())) {
                throw new BusinessRuleException("Seller does not own the item");
            }

        } else {

            if (dto.itemName == null || dto.itemName.trim().isEmpty()) {
                throw new BusinessRuleException("itemName is required");
            }

            if (dto.startPrice <= 0) {
                throw new BusinessRuleException("startPrice must be positive");
            }

            Item newItem = new Item();

            newItem.setName(dto.itemName.trim());
            newItem.setDescription(
                    dto.itemDescription == null
                            ? ""
                            : dto.itemDescription.trim()
            );

            newItem.setStartingPrice(dto.startPrice);
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

        // FIX QUAN TRỌNG
        auction.setItemId(item.getId());

        auction.setStartTime(dto.startTime);
        auction.setEndTime(dto.endTime);

        auction.setCurrentPrice(item.getStartingPrice());

        auction.setCreatedBy(sellerId);

        if (dto.startTime.isAfter(now)) {
            auction.setState(AuctionState.SCHEDULED);
        } else if (!dto.endTime.isBefore(now)) {
            auction.setState(AuctionState.ACTIVE);
        } else {
            throw new BusinessRuleException("endTime must be in the future");
        }

        return auctionRepository.save(auction);
    }

    @Override
    public List<Auction> listAuctionsByState(AuctionState state) {

        if (state == null) {
            return listAuctions();
        }

        return auctionRepository.findByState(state);
    }

    @Override
    public void validateAuctionOpenForBidding(UUID auctionId) {

        Auction a = getAuction(auctionId);

        if (a.getState() != AuctionState.SCHEDULED
                && a.getState() != AuctionState.ACTIVE) {

            throw new BusinessRuleException("Auction is not open for bidding");
        }

        Instant now = Instant.now();

        if (now.isBefore(a.getStartTime())
                || now.isAfter(a.getEndTime())) {

            throw new BusinessRuleException("Auction is not within active time window");
        }
    }

    @Override
    @Transactional
    public void startAuction(UUID auctionId) {

        Auction a = getAuction(auctionId);

        if (a.getState() != AuctionState.SCHEDULED) {
            throw new BusinessRuleException("Auction not in OPEN state");
        }

        a.setState(AuctionState.ACTIVE);

        auctionRepository.save(a);
    }

    @Scheduled(fixedDelayString = "${auction.state.refresh.ms:10000}")
    public void scheduledRefreshStates() {

        try {
            refreshStates();
        } catch (Exception ex) {
            System.err.println("Error refreshing auction states: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void refreshStates() {

        Instant now = Instant.now();

        List<Auction> toStart =
                auctionRepository.findByStateAndStartTimeBefore(
                        AuctionState.SCHEDULED,
                        now
                );

        for (Auction a : toStart) {

            a.setState(AuctionState.ACTIVE);

            auctionRepository.save(a);
        }

        List<Auction> toFinish =
                auctionRepository.findByStateAndEndTimeBefore(
                        AuctionState.ACTIVE,
                        now
                );

        for (Auction a : toFinish) {

            a.setState(AuctionState.FINISHED);

            a.setWinnerId(a.getLeaderId());

            auctionRepository.save(a);
        }
    }
}