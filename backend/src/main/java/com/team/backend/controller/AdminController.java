package com.team.backend.controller;

import com.team.backend.dto.AdminCreateDto;
import com.team.backend.dto.UserDto;
import com.team.backend.entity.User;
import com.team.backend.service.UserService;
import com.team.backend.service.AuctionService;
import com.team.backend.entity.Auction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final AuctionService auctionService;

    public AdminController(UserService userService, AuctionService auctionService) {
        this.userService = userService;
        this.auctionService = auctionService;
    }

    // Tạo admin mới (chỉ dùng trong dev hoặc qua auth admin)
    @PostMapping("/create")
    public ResponseEntity<UserDto> createAdmin(@RequestBody AdminCreateDto dto) {
        User admin = userService.createAdmin(dto.username, dto.password, dto.superAdmin);
        UserDto out = toDto(admin);
        return ResponseEntity.ok(out);
    }

    // Lấy danh sách tất cả auctions (ví dụ admin muốn xem)
    @GetMapping("/auctions")
    public ResponseEntity<?> listAuctions() {
        return ResponseEntity.ok(auctionService.listAuctions()
                .stream().map(a -> {
                    // convert to minimal DTO
                    return a.getId();
                }).collect(Collectors.toList()));
    }

    // Force close auction (admin action)
    @PostMapping("/auctions/{id}/force-close")
    public ResponseEntity<?> forceClose(@PathVariable("id") UUID auctionId) {
        auctionService.closeAuction(auctionId);
        return ResponseEntity.ok().build();
    }

    private UserDto toDto(User u) {
        UserDto d = new UserDto();
        d.id = u.getId();
        d.username = u.getUsername();
        d.role = u.getRole();
        return d;
    }
}
