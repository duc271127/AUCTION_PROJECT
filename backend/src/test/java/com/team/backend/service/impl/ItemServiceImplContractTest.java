package com.team.backend.service.impl;

import com.team.backend.dto.ItemCreateRequest;
import com.team.backend.dto.ItemResponse;
import com.team.backend.dto.PublicItemDetailDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.FavoriteRepository;
import com.team.backend.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ItemServiceImplContractTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AutoBidRepository autoBidRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Test
    void createForSeller_keepsPrimaryImageFirstAndReturnsMultiImageContract() {
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository, auctionRepository, autoBidRepository, favoriteRepository);
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
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository, auctionRepository, autoBidRepository, favoriteRepository);
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
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository, auctionRepository, autoBidRepository, favoriteRepository);
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

    @Test
    void deleteForSeller_allowsScheduledAuctionBeforeStartTime() {
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository, auctionRepository, autoBidRepository, favoriteRepository);
        UUID sellerId = UUID.randomUUID();
        ItemResponse saved = itemService.createForSeller(sellerId, baseRequest());

        Auction auction = new Auction();
        auction.setItemId(saved.getId());
        auction.setTitle("Camera Auction");
        auction.setDescription("Auction for camera");
        auction.setImageUrl(saved.getImagePath());
        auction.setCategory(saved.getCategory());
        auction.setStartTime(Instant.now().plusSeconds(3600));
        auction.setEndTime(Instant.now().plusSeconds(7200));
        auction.setCurrentPrice(saved.getStartingPrice());
        auction.setReservePrice(saved.getReservePrice());
        auction.setSellerId(sellerId);
        auction.setState(AuctionState.SCHEDULED);
        auctionRepository.save(auction);

        itemService.deleteForSellerResponse(saved.getId(), sellerId);

        assertTrue(itemRepository.findById(saved.getId()).isEmpty());
        assertTrue(auctionRepository.findByItemId(saved.getId()).isEmpty());
    }

    @Test
    void deleteForSeller_rejectsAuctionThatIsAlreadyLive() {
        ItemServiceImpl itemService = new ItemServiceImpl(itemRepository, auctionRepository, autoBidRepository, favoriteRepository);
        UUID sellerId = UUID.randomUUID();
        ItemResponse saved = itemService.createForSeller(sellerId, baseRequest());

        Auction auction = new Auction();
        auction.setItemId(saved.getId());
        auction.setTitle("Camera Auction");
        auction.setDescription("Auction for camera");
        auction.setImageUrl(saved.getImagePath());
        auction.setCategory(saved.getCategory());
        auction.setStartTime(Instant.now().minusSeconds(60));
        auction.setEndTime(Instant.now().plusSeconds(3600));
        auction.setCurrentPrice(saved.getStartingPrice());
        auction.setReservePrice(saved.getReservePrice());
        auction.setSellerId(sellerId);
        auction.setState(AuctionState.ACTIVE);
        auctionRepository.save(auction);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> itemService.deleteForSellerResponse(saved.getId(), sellerId));

        assertEquals("This listing cannot be deleted because the auction is already live.", exception.getMessage());
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
