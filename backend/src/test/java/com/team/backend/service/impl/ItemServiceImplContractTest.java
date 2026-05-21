package com.team.backend.service.impl;

import com.team.backend.dto.ItemCreateRequest;
import com.team.backend.dto.ItemResponse;
import com.team.backend.dto.PublicItemDetailDto;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ItemServiceImplContractTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void createForSeller_keepsPrimaryImageFirstAndReturnsMultiImageContract() {
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository);
        ItemCreateRequest request = baseRequest();
        request.setImagePath("/api/uploads/images/main.png");
        request.setImageUrls(List.of(
                "/api/uploads/images/second.png",
                "/api/uploads/images/main.png",
                "/api/uploads/images/third.png"
        ));

        ItemResponse response = itemService.createForSeller(UUID.randomUUID(), request);

        assertEquals("/api/uploads/images/main.png", response.getImagePath());
        assertEquals(List.of(
                "/api/uploads/images/main.png",
                "/api/uploads/images/second.png",
                "/api/uploads/images/third.png"
        ), response.getImageUrls());
    }

    @Test
    void createForSeller_rejectsNegativeQuantityAndLocalImagePath() {
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository);
        ItemCreateRequest quantityRequest = baseRequest();
        quantityRequest.setQuantity(-1);

        assertThrows(BusinessRuleException.class,
                () -> itemService.createForSeller(UUID.randomUUID(), quantityRequest));

        ItemCreateRequest localPathRequest = baseRequest();
        localPathRequest.setImagePath("C:\\Users\\seller\\picture.png");

        assertThrows(BusinessRuleException.class,
                () -> itemService.createForSeller(UUID.randomUUID(), localPathRequest));
    }

    @Test
    void publicItemDetail_contractDoesNotExposeReservePrice() {
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository);
        ItemCreateRequest request = baseRequest();
        request.setReservePrice(900.0);

        ItemResponse saved = itemService.createForSeller(UUID.randomUUID(), request);
        PublicItemDetailDto detail = itemService.getPublicItemDetail(saved.getId());

        assertEquals(saved.getId(), detail.getId());
        assertEquals("Camera", detail.getProductName());
        assertTrue(Arrays.stream(PublicItemDetailDto.class.getDeclaredFields())
                .map(Field::getName)
                .noneMatch("reservePrice"::equals));
    }

    private ItemCreateRequest baseRequest() {
        ItemCreateRequest request = new ItemCreateRequest();
        request.setProductName("Camera");
        request.setDescription("Test listing");
        request.setCategory("Electronics");
        request.setStartingPrice(100.0);
        request.setReservePrice(120.0);
        request.setQuantity(1);
        request.setSku("CAM-001");
        return request;
    }
}
