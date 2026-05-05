package com.team.backend.dto;

import java.util.UUID;

/**
 * DTO trả về cho admin khi lấy danh sách item chờ duyệt.
 * Chỉ chứa các trường cần thiết cho UI admin.
 */
public class PendingItemDto {
    public UUID id;
    public UUID sellerId;
    public String description;
    public String category;
    public double startingPrice;
    public Double reservePrice;
    public String status;
    public String imagePath;   // optional
}
