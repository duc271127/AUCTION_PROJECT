package com.example.auction.controller;

import com.example.auction.entity.Auction;
import com.example.auction.repository.AuctionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auctions")
public class AuctionController {

    private final AuctionRepository repo;

    public AuctionController(AuctionRepository repo) {
        this.repo = repo;
    }

    // 1. LIST
    @GetMapping
    public List<Auction> getAll() {
        return repo.findAll();
    }

    // 2. DETAIL
    @GetMapping("/{id}")
    public Auction getById(@PathVariable Long id) {
        return repo.findById(id).orElse(null);
    }

    // 3. CREATE
    @PostMapping
    public Auction create(@RequestBody Auction auction) {
        auction.setCurrentPrice(auction.getStartPrice());
        return repo.save(auction);
    }
}
