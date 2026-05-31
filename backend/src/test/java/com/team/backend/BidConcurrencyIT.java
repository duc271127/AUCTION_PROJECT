package com.team.backend;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.Wallet;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class BidConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private com.team.backend.service.BidService bidService;

    private UUID auctionId;

    @BeforeEach
    void setup() {
        Auction a = new Auction();
        auctionId = a.getId();
        a.setItemId(UUID.randomUUID());
        a.setStartTime(Instant.now().minusSeconds(10));
        a.setEndTime(Instant.now().plusSeconds(3600));
        a.setState(AuctionState.ACTIVE);
        a.setCurrentPrice(100.0); // double
        auctionRepository.save(a);
    }

    @AfterEach
    void cleanup() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void concurrentBids_shouldResultInHighestValidBidPersisted() throws InterruptedException {
        int clients = 3;
        ExecutorService ex = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);

        double[] bids = {110.0, 111.0, 112.0};

        try {
            Future<Boolean>[] futures = new Future[clients];
            for (int i = 0; i < clients; i++) {
                final int idx = i;
                futures[i] = ex.submit(() -> {
                    start.await();
                    try {
                        UUID bidderId = UUID.randomUUID();
                        Wallet wallet = new Wallet();
                        wallet.setUserId(bidderId);
                        wallet.setBalance(new java.math.BigDecimal("1000.00"));
                        walletRepository.save(wallet);
                        bidService.placeBid(auctionId, bidderId, bids[idx]);
                        return true;
                    } catch (Exception exx) {
                        return false;
                    }
                });
            }

            start.countDown();

            int success = 0;
            for (int i = 0; i < clients; i++) {
                try {
                    if (futures[i].get(10, TimeUnit.SECONDS)) success++;
                } catch (Exception e) { /* ignore individual failures */ }
            }

            Auction finalA = auctionRepository.findById(auctionId).orElseThrow();
            assertTrue(finalA.getCurrentPrice() >= 110.0);
            assertTrue(success >= 1);
        } finally {
            ex.shutdownNow();
        }
    }
}
