package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.service.AuctionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;

    public AuctionServiceImpl(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Override
    @Transactional
    public Auction createAuction(Auction auction) {
        if (auction.getStartTime() == null || auction.getEndTime() == null) {
            throw new BusinessRuleException("Start time and end time are required");
        }
        auction.setState(AuctionState.OPEN);
        auction.setCurrentPrice(auction.getItem().getStartPrice());
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
}
