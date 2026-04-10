package com.team.backend.controller;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.BidRequestDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.Item;
import com.team.backend.entity.BidTransaction;
import com.team.backend.service.AuctionService;
import com.team.backend.service.BidService;
import com.team.backend.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final ItemRepository itemRepository;

    public AuctionController(AuctionService auctionService, BidService bidService, ItemRepository itemRepository) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.itemRepository = itemRepository;
    }

    @PostMapping
    public ResponseEntity<AuctionDto> createAuction(@RequestBody AuctionCreateDto dto) {
        Item item = new Item();
        item.setName(dto.itemName);
        item.setDescription(dto.itemDescription);
        item.setStartPrice(dto.startPrice);
        itemRepository.save(item);

        Auction a = new Auction();
        a.setItem(item);
        a.setStartTime(dto.startTime);
        a.setEndTime(dto.endTime);
        a.setCurrentPrice(dto.startPrice);
        a.setState(com.team.backend.entity.AuctionState.OPEN);

        Auction saved = auctionService.createAuction(a);
        AuctionDto out = toDto(saved);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<?> placeBid(@PathVariable("id") UUID auctionId, @RequestBody BidRequestDto dto) {
        BidTransaction tx = bidService.placeBid(auctionId, dto.bidderId, dto.amount);
        Auction a = auctionService.getAuction(auctionId);
        return ResponseEntity.ok(toDto(a));
    }

    private AuctionDto toDto(Auction a) {
        AuctionDto d = new AuctionDto();
        d.id = a.getId();
        d.itemId = a.getItem().getId();
        d.itemName = a.getItem().getName();
        d.currentPrice = a.getCurrentPrice();
        d.leaderId = a.getLeaderId();
        d.startTime = a.getStartTime();
        d.endTime = a.getEndTime();
        d.state = a.getState().name();
        return d;
    }
}
