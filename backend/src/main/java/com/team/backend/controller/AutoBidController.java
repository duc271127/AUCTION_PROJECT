package com.team.backend.controller;

import com.team.backend.dto.AutoBidRequestDto;
import com.team.backend.entity.AutoBid;
import com.team.backend.service.AutoBidService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auctions/{auctionId}/auto-bid")
public class AutoBidController {

    private final AutoBidService autoBidService;

    public AutoBidController(AutoBidService autoBidService) {
        this.autoBidService = autoBidService;
    }

    @PostMapping
    public ResponseEntity<AutoBid> setAutoBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID bidderId,
            @Valid @RequestBody AutoBidRequestDto dto
    ) {
        AutoBid autoBid = autoBidService.setAutoBid(
                auctionId,
                bidderId,
                dto.getMaxAmount()
        );

        return ResponseEntity.ok(autoBid);
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelAutoBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID bidderId
    ) {
        autoBidService.cancelAutoBid(auctionId, bidderId);
        return ResponseEntity.noContent().build();
    }
}
