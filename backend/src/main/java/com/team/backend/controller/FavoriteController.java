package com.team.backend.controller;

import com.team.backend.dto.AuctionDetailResponse;
import com.team.backend.dto.FavoriteRequest;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.service.FavoriteService;
import com.team.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;

    public FavoriteController(FavoriteService favoriteService, UserService userService) {
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<List<AuctionDetailResponse>> listFavorites() {
        return ResponseEntity.ok(favoriteService.list(resolveCurrentUserId()));
    }

    @PostMapping("/{auctionId}")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<Void> addFavorite(@PathVariable UUID auctionId) {
        favoriteService.add(resolveCurrentUserId(), auctionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<Void> addFavoriteLegacy(@RequestBody FavoriteRequest request) {
        if (request == null || request.auctionId == null) {
            throw new BusinessRuleException("auctionId is required");
        }
        favoriteService.add(resolveCurrentUserId(), request.auctionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{auctionId}")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID auctionId) {
        favoriteService.remove(resolveCurrentUserId(), auctionId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is required");
        }

        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user not found: " + auth.getName());
        }
        return user.getId();
    }
}
