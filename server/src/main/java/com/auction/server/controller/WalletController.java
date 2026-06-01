package com.auction.server.controller;

import com.auction.server.dto.WalletAmountRequest;
import com.auction.server.dto.WalletBalanceDto;
import com.auction.server.dto.WalletTransactionDto;
import com.auction.server.entity.User;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.service.UserService;
import com.auction.server.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceDto> getBalance() {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletBalanceDto> deposit(@RequestBody WalletAmountRequest request) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(walletService.deposit(userId, request.getAmount()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletBalanceDto> withdraw(@RequestBody WalletAmountRequest request) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(walletService.withdraw(userId, request.getAmount()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<WalletTransactionDto>> getHistory() {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(walletService.getHistory(userId));
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user is required");
        }

        User user = userService.findByUsername(auth.getName());

        if (user == null) {
            throw new BusinessRuleException("Authenticated user not found: " + auth.getName());
        }

        return user.getId();
    }
}

