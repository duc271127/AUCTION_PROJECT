package com.team.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Payload to create an Item.
 */
public class ItemCreateDto {
    @NotBlank
    public String name;

    public String description;

    @Positive
    public double startPrice;
}
