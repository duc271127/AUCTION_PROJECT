# Ôn Tập Bảo Vệ 3 Vai Trò - Auction Project

## 1. Phạm vi ôn tập

Tài liệu này được chốt dựa trên:

- yêu cầu backend/UI trong `docs/backend-ui-requirements.docx`
- luồng realtime và concurrency trong `docs/hardcore/*.md`
- code hiện tại của `backend/` và phần client JavaFX trong `ui/`

Mục tiêu của file này không phải học thuộc API. Mục tiêu là để 3 người trả lời được:

- hệ thống đang chạy theo kiến trúc gì
- tại sao nhóm chọn cấu trúc đó
- luồng dữ liệu đi từ UI tới DB và quay lại realtime như thế nào
- phần nào đã đáp ứng requirement
- phần nào đang là trade-off, tạm thời cho demo, hoặc còn phải cải tiến

## 2. Ảnh chụp hệ thống hiện tại

### 2.1 Kiến trúc tổng quan

- `ui/`: JavaFX client, gọi REST và mở socket realtime.
- `backend/`: Spring Boot, Spring Security, JPA, PostgreSQL.
- Auth: JWT stateless.
- Data chính: `User`, `Item`, `Auction`, `BidTransaction`, `AutoBid`, `Favorite`, `Wallet`, `WalletTransaction`.
- Realtime: STOMP over WebSocket, topic `/topic/auctions/{auctionId}`.
- Scheduler: tự refresh state auction theo thời gian.

### 2.2 Luồng nghiệp vụ chính

1. Seller tạo `Item`.
2. Admin duyệt `Item`.
3. Admin hoặc seller tạo `Auction` từ `Item`.
4. Client load danh sách auction qua REST.
5. Client vào trang live bidding, lấy snapshot qua REST trước.
6. Client subscribe socket vào topic của auction.
7. Bidder gửi bid qua REST `POST /api/auctions/{id}/bids`.
8. Backend lock/transaction, validate, cập nhật giá và leader, ghi history, xử lý auto-bid, anti-sniping.
9. Sau commit, backend broadcast event realtime cho các client đang xem.

### 2.3 Những file phải nắm rất chắc

#### Chung

- `backend/src/main/java/com/team/backend/controller/AuctionController.java`
- `backend/src/main/java/com/team/backend/service/impl/AuctionServiceImpl.java`
- `backend/src/main/java/com/team/backend/service/impl/BidServiceImpl.java`
- `backend/src/main/java/com/team/backend/service/impl/BidTransactionalService.java`
- `backend/src/main/java/com/team/backend/service/impl/AutoBidServiceImpl.java`
- `backend/src/main/java/com/team/backend/config/SecurityConfig.java`
- `backend/src/main/java/com/team/backend/config/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/team/backend/config/WebSocketConfig.java`
- `backend/src/main/java/com/team/backend/realtime/RealtimeNotifier.java`
- `backend/src/main/java/com/team/backend/service/impl/StompEventPublisher.java`
- `backend/src/main/java/com/team/backend/entity/Auction.java`
- `backend/src/main/java/com/team/backend/repository/AuctionRepository.java`
- `backend/src/test/java/com/team/backend/BidConcurrencyIT.java`
- `backend/src/test/java/com/team/backend/AutoBidEdgeCasesTest.java`

#### Nếu là Backend

- `backend/src/main/java/com/team/backend/controller/AdminController.java`
- `backend/src/main/java/com/team/backend/controller/SellerItemController.java`
- `backend/src/main/java/com/team/backend/service/impl/ItemServiceImpl.java`
- `backend/src/main/java/com/team/backend/service/impl/AdminServiceImpl.java`
- `backend/src/main/java/com/team/backend/service/impl/UserServiceImpl.java`

#### Nếu là Hardcore

- `docs/hardcore/auction-lock-manager-design.md`
- `docs/hardcore/thread-safe-areas-in-bid-flow.md`
- `docs/hardcore/realtime-architecture.md`
- `backend/src/main/java/com/team/backend/service/impl/BidServiceImpl.java`
- `backend/src/main/java/com/team/backend/service/impl/BidTransactionalService.java`

#### Nếu là Server + Data

- `backend/src/main/java/com/team/backend/entity/*.java`
- `backend/src/main/java/com/team/backend/repository/*.java`
- `backend/src/main/java/com/team/backend/service/impl/WalletServiceImpl.java`
- `backend/src/main/java/com/team/backend/entity/OutboxEvent.java`
- `backend/src/main/java/com/team/backend/service/impl/OutboxDispatcher.java`

## 3. Những điểm rất dễ bị hỏi hoặc bị phản biện

### 3.1 Lệch giữa requirement và code hiện tại

- Requirement thiên về tên state `OPEN/CLOSED`, còn code đang dùng `ACTIVE/FINISHED`.
- Requirement gợi ý endpoint favorite kiểu `/api/users/me/saved-auctions`, nhưng code đang dùng `/api/favorites`.
- Requirement muốn auto-bid status riêng, nhưng code hiện có `DELETE /auto-bid`, `GET /auto-bids`, `GET /me/auto-bids`.
- Requirement muốn detail giàu dữ liệu hơn như gallery, estimate range, seller avatar, condition; code detail hiện còn gọn.
- Requirement nói admin reject có thể kèm `reason`; code hiện reject chưa nhận reason.

### 3.2 Những trade-off hiện tại trong code

- Bid dùng `double`, wallet dùng `BigDecimal`.
- Realtime đang dùng Spring Simple Broker, phù hợp demo nhưng chưa phải lựa chọn scale production.
- Bid có chiến lược fallback sang `in-memory lock`; cách này chỉ an toàn trong một process JVM, không giải quyết được multi-instance.
- Có `OutboxEvent` và `OutboxDispatcher`, nhưng luồng bid realtime chính vẫn đang phát thẳng qua `StompEventPublisher` sau commit, chưa đi full outbox.
- `spring.jpa.hibernate.ddl-auto=update` tiện cho phát triển, nhưng không phải cách migration bài bản khi lên môi trường nghiêm túc.

### 3.3 Những điểm có thể bị hỏi sâu

- Tại sao `Auction` vừa có `itemId` vừa có `@ManyToOne Item`.
- Tại sao nhiều field trong `Auction` là `@Transient`.
- Tại sao validate bid phải nằm trong vùng đã lock, không validate hết ở controller.
- Tại sao event realtime nên bắn sau commit.
- Tại sao `AutoBid` có unique `(auction_id, bidder_id)`.
- Tại sao `WalletRepository` dùng pessimistic write.
- Tại sao test concurrency hiện tại mới chứng minh được một phần chứ chưa phải stress test thực sự.

## 4. Vai trò 1 - Backend

### 4.1 Phần Backend phải kể trôi chảy

Người Backend phải kể được mạch sau trong 2-3 phút:

- hệ thống chia `controller -> service -> repository -> entity -> dto`
- auth đi bằng JWT stateless
- seller tạo item, admin duyệt, từ item tạo auction
- bidder xem list/detail, đặt bid, xem history
- realtime không thay REST, mà bổ sung cho live bidding
- các DTO đang được thiết kế để UI nhận dữ liệu ổn định hơn entity raw

### 4.2 Câu hỏi ôn tập rất kỹ cho Backend

1. Hãy mô tả toàn bộ kiến trúc backend hiện tại theo package và trách nhiệm của từng tầng.
2. Vì sao controller không làm business logic trực tiếp mà đẩy xuống service?
3. Vì sao `AuctionController` phải map entity sang `AuctionDto` và `AuctionDetailResponse` thay vì trả thẳng entity?
4. Khác nhau giữa `AuctionDto`, `AuctionDetailResponse`, `AuctionSummaryResponse`, `BidPlacementResponse` là gì?
5. Tại sao `SecurityConfig` cho `GET /api/auctions/**` là public nhưng `POST /api/auctions/{id}/bids` lại bắt buộc role?
6. `SecurityConfig` và `@PreAuthorize` bổ sung cho nhau như thế nào?
7. `JwtAuthenticationFilter` lấy token, parse username, load `UserDetails`, rồi set `SecurityContext` theo trình tự nào?
8. Nếu token sai hoặc hết hạn thì backend đang xử lý thế nào?
9. Luồng `register` và `login` đang hỗ trợ theo `username` và theo `email` ra sao?
10. Vì sao `User` được thiết kế theo `SINGLE_TABLE` inheritance cho `Admin`, `Seller`, `Bidder`?
11. Tại sao `role` trong `User` hiện là `String` chứ chưa dùng enum?
12. Hãy đi từ `POST /seller/items/v2` đến lúc item được lưu xuống DB và trả về JSON.
13. Vì sao khi seller update item thì code lại reset item về `PENDING` và xóa thông tin duyệt cũ?
14. Hãy giải thích validation của `ItemServiceImpl` cho `imagePath` và `imageUrls`. Vì sao backend chặn local file path?
15. Luồng admin duyệt item và tạo auction từ item hoạt động như thế nào?
16. Tại sao `AdminServiceImpl` không tự thao tác hết với `AuctionRepository` mà gọi sang `AuctionService`?
17. `AuctionServiceImpl.createAuction` đang lấy dữ liệu từ `Item` sang `Auction` như thế nào?
18. Vì sao `Auction` có cả `title`, `description`, `category`, `imageUrl` riêng dù `Item` cũng có dữ liệu tương tự?
19. Khi tạo auction, hệ thống quyết định `SCHEDULED` hay `ACTIVE` theo logic nào?
20. `refreshStates()` và `scheduledRefreshStates()` có vai trò gì? Nếu không có scheduler thì hệ quả là gì?
21. Hãy giải thích cách backend phục vụ Home, Trending, For You bằng `searchCatalog`, `searchTrendingCatalog`, `searchPersonalizedCatalog`.
22. `computeTrendingScore` đang dùng các tín hiệu nào? Vì sao nhóm chọn các trọng số đó?
23. `For You` hiện tại có phải recommendation thật không? Nếu chưa thì đang dùng chiến lược tạm nào?
24. `FavoriteController` liên hệ thế nào với tính năng `For You` và `favoriteCount` trên auction?
25. Khi client gọi `GET /api/auctions/{id}/detail`, backend tăng `viewCount` ở đâu và có tăng tự động hay không?
26. `AuctionMapper.toDetail` đang thiếu những field nào so với requirement? Nếu có thêm gallery hoặc estimate thì sẽ gắn vào đâu?
27. Khi đóng auction thủ công qua `POST /api/auctions/{id}/close`, backend làm những bước nào trước khi bắn event đóng auction?
28. Các exception nghiệp vụ chính của project là gì và chúng đang được convert thành HTTP response như thế nào?
29. Nếu UI hỏi "contract nào là ổn định, contract nào còn thay đổi", bạn sẽ trả lời ra sao?
30. Nếu giảng viên hỏi "vì sao không gộp hết list/detail/summary thành một DTO", bạn trả lời thế nào?

### 4.3 Câu hỏi phản biện cho Backend

1. Requirement muốn auction detail có `images[]`, còn code mới có `imageUrl`; nhóm định mở rộng backward-compatible thế nào?
2. Requirement muốn endpoint reject có `reason`, nếu thêm bây giờ thì sửa DTO và service ở đâu?
3. Requirement dùng `saved-auctions`, nhưng code dùng `favorites`; nhóm chọn rename API hay map lại ở UI?
4. Nếu cần thêm filter `category`, `keyword`, `sort`, `page`, `size` cho tất cả màn hình catalog, backend hiện tại đã đủ chưa?
5. Nếu phải demo seller dashboard số liệu tổng hợp, phần nào của code đã có sẵn, phần nào còn thiếu?

### 4.4 Backend phải tự trả lời được

- vì sao chia DTO nhiều lớp
- vì sao giữ entity sạch, không trả thẳng ra ngoài
- vì sao auth là stateless
- vì sao scheduler cần tồn tại dù bid flow đã kiểm tra `endTime`
- vì sao current contract đang phục vụ UI demo tốt hơn việc expose schema DB thô

## 5. Vai trò 2 - Hardcore

### 5.1 Phần Hardcore phải kể trôi chảy

Người Hardcore phải kể được mạch sau:

- bidding là shared-state problem
- shared state gồm `currentPrice`, `leaderId`, `bidHistory`, `endTime`, `state`
- chỉ lock theo từng auction, không lock toàn hệ thống
- code hiện ưu tiên row lock trong DB, có retry, và fallback sang in-memory lock
- event realtime chỉ phát sau khi transaction commit
- auto-bid và anti-sniping phải bám cùng state machine của auction

### 5.2 Câu hỏi ôn tập rất kỹ cho Hardcore

1. Tại sao bidding là phần khó nhất của dự án này?
2. Hãy chỉ ra chính xác shared state của một auction trong code hiện tại.
3. Nếu không có lock hoặc transaction đúng cách thì race condition nào có thể xảy ra?
4. Tại sao `BidServiceImpl` không tự update auction ngay mà gọi `BidTransactionalService`?
5. Vì sao `placeBid` có vòng `for` retry?
6. Khi nào code gặp `ObjectOptimisticLockingFailureException` và khi nào gặp `PessimisticLockingFailureException`?
7. Tại sao `Auction` có `@Version` nhưng vẫn dùng `findByIdForUpdate` với `PESSIMISTIC_WRITE`?
8. Ý tưởng của nhóm là ưu tiên optimistic hay pessimistic, hay là kết hợp? Vì sao?
9. Nếu `findByIdForUpdate` đã khóa row rồi thì vai trò thực tế của `@Version` còn là gì?
10. Tại sao validation bid phải nằm sau khi lấy auction đã lock, không nên validate hoàn toàn ở controller?
11. Tại sao `minAllowed = currentPrice + minIncrement` phải được tính trên state mới nhất bên trong transaction?
12. Anti-sniping đang chạy trong cùng transaction như thế nào? Vì sao phải làm vậy?
13. Nếu hai bid tới trong 10 giây cuối cùng, làm sao tránh gia hạn thời gian sai?
14. `previousLeader` được lưu lại để làm gì trong quá trình publish event?
15. Tại sao event được `registerSynchronization(... afterCommit())` thay vì bắn ngay trong transaction?
16. Nếu bắn realtime trước commit mà commit fail thì UI sẽ gặp vấn đề gì?
17. Hãy mô tả chính xác một vòng manual bid rồi auto-bid nối tiếp trong `BidTransactionalService`.
18. `MAX_AUTO_BID_ROUNDS = 10` có ý nghĩa gì? Nếu không có ngưỡng này thì rủi ro nào xảy ra?
19. Tie-break của auto-bid bằng nhau đang được thực hiện ra sao?
20. Vai trò của `findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc` là gì trong tie-break?
21. Tại sao code fallback sang `placeBidWithInMemoryLock`?
22. `lockMap.computeIfAbsent(auctionId, ...)` giải quyết điều gì và có nhược điểm gì?
23. Fallback `in-memory lock` đúng trong trường hợp nào và sai khi scale theo trường hợp nào?
24. Nếu deploy 2 instance backend sau load balancer thì `ReentrantLock` trong RAM còn đủ an toàn không?
25. Nếu scheduler đang đóng auction cùng lúc với một bid tới vào đúng thời điểm cuối, trạng thái nào có thể tranh chấp?
26. `READ_COMMITTED` trong transaction có đủ cho case hiện tại không? Vì sao?
27. Vì sao broadcast socket không nên giữ lock quá lâu?
28. Nếu cần tách hẳn `AuctionLockManager` như tài liệu hardcore mô tả, bạn sẽ refactor ở lớp nào?
29. Nếu chuyển sang broker ngoài hoặc event bus ngoài, concurrency core có thay đổi không?
30. Hiện test concurrency đã chứng minh được điều gì và chưa chứng minh được điều gì?

### 5.3 Các tình huống bị hỏi xoáy cho Hardcore

1. Hai bidder cùng gửi một mức giá bằng nhau thì ai thắng và vì sao?
2. Hai auto-bid cùng `maxAmount` thì rule hiện tại chọn ai?
3. Nếu DB lock fail nhưng fallback lock thành công, consistency có bị lệch với DB transaction strategy không?
4. Nếu event realtime gửi chậm hơn REST response, UI có thể thấy trạng thái nào trước?
5. Nếu `lockMap` cứ lớn dần theo số lượng auction cũ, có cần cleanup không?
6. Nếu `autoBidRepository` trả về dữ liệu stale hoặc không có index tốt, phần nào của bidding bị chậm?
7. Nếu muốn chống duplicate request do client retry, hệ thống hiện có idempotency key chưa?

### 5.4 Hardcore phải tự trả lời được

- tại sao shared-state phải chốt trong một transaction/critical section
- tại sao row lock tốt hơn global lock
- tại sao fallback lock chỉ là phương án cứu cánh cho demo
- tại sao auto-bid là bài toán state machine chứ không chỉ là một if/else
- tại sao realtime phải đi sau state đúng, không được đi trước

## 6. Vai trò 3 - Server + Data

### 6.1 Phần Server + Data phải kể trôi chảy

Người Server + Data phải kể được:

- backend dùng PostgreSQL và JPA để persist domain
- schema được tổ chức quanh các aggregate chính: user, item, auction, bid, wallet
- repository có cả query đơn giản và query có lock
- phần data phục vụ không chỉ CRUD mà còn phục vụ ranking, audit, realtime, wallet
- một số quyết định hiện tại phục vụ tốc độ phát triển demo, chưa phải production-grade hoàn toàn

### 6.2 Câu hỏi ôn tập rất kỹ cho Server + Data

1. Vì sao nhóm chọn PostgreSQL cho dự án này?
2. Những bảng quan trọng nhất của hệ thống là gì và quan hệ logic giữa chúng ra sao?
3. Vì sao project dùng UUID cho entity thay vì `Long auto-increment`?
4. `Auction` vì sao giữ cả `itemId` primitive lẫn quan hệ `@ManyToOne Item`?
5. `Item.imageUrls` dùng `@ElementCollection` vào bảng `item_images` có lợi gì và hạn chế gì?
6. `BidTransaction` có index nào và vì sao cần index theo `auction_id` và `bidder_id`?
7. Nếu traffic tăng mạnh, bảng nào sẽ tăng nhanh nhất và cần tối ưu sớm nhất?
8. `AuctionRepository.findByIdForUpdate` đang khóa ở cấp nào và phục vụ ca sử dụng nào?
9. `WalletRepository.findByUserIdForUpdate` giải quyết bài toán gì?
10. Vì sao money trong wallet dùng `BigDecimal` nhưng bidding lại dùng `double`? Đây là điểm yếu gì?
11. Nếu phải sửa bidding sang `BigDecimal`, bạn sẽ sửa từ entity, DTO, validation, query, test như thế nào?
12. `Favorite`, `viewCount`, `bidCount` đang được dùng để tính score như thế nào? Nếu cần tối ưu query thì làm gì?
13. `searchCatalog` hiện để DB lọc, nhưng `trending` và `for-you` lại lấy nhiều dữ liệu rồi sort ở service. Cách này ổn đến mức nào?
14. Nếu số auction lên hàng trăm nghìn, `rankCatalog` hiện tại có bottleneck gì?
15. `resolvePreferredCategories` có nguy cơ N+1 query không? Vì sao?
16. `Auction.populateTransientFields` mỗi auction lại đếm bid và favorite, điều này có thể tạo loại chi phí gì?
17. Nếu muốn giảm N+1, bạn sẽ chọn projection query, aggregate query, materialized view hay cache?
18. Vì sao `OutboxEvent` được tạo như một bảng riêng? Mục tiêu thiết kế của outbox pattern là gì?
19. `OutboxDispatcher` hiện đọc tất cả pending rồi cắt batch 50. Cách này có điểm mạnh và điểm yếu gì?
20. Vì sao hiện tại luồng bid realtime chưa thật sự đi full outbox?
21. Nếu muốn scale realtime nhiều instance, Simple Broker có còn phù hợp không?
22. Nếu backend chạy nhiều instance, event broadcast phải được phối hợp qua thành phần ngoài nào?
23. `spring.jpa.hibernate.ddl-auto=update` thuận tiện gì cho phát triển và nguy hiểm gì cho production?
24. Nếu cần migration chuẩn, nhóm sẽ dùng Flyway hay Liquibase, và tại sao?
25. Tính nhất quán dữ liệu của `winnerId`, `leaderId`, `currentPrice`, `bid history` đang được đảm bảo ở đâu?
26. Ràng buộc "một item không nằm trong hai auction active cùng lúc" hiện được đảm bảo chặt đến mức nào?
27. Unique constraint của `AutoBid` trên `(auction_id, bidder_id)` ngăn loại lỗi nào?
28. Nếu cần soft delete cho item hoặc auction, schema và query phải đổi ra sao?
29. Nếu cần lưu audit sâu hơn cho bid reject, bạn sẽ thêm bảng mới hay thêm cột vào bảng cũ?
30. Nếu cần archive bid history cũ để giảm tải, bạn sẽ partition bảng, archive table, hay event stream?

### 6.3 Câu hỏi phản biện cho Server + Data

1. Vì sao `Auction` không lưu sẵn `sellerName`, `leaderName` trong DB mà phải resolve động?
2. Nếu `displayName` của user đổi, dữ liệu lịch sử bid nên hiện tên mới hay tên cũ tại thời điểm bid?
3. Nếu cần chính xác tài chính, vì sao money không nên dùng `double`?
4. Nếu `viewCount` được update quá thường xuyên, có cần tách sang Redis hoặc async counter không?
5. Nếu `favoriteCount` và `bidCount` cần hiển thị rất nhanh trên list page, có nên denormalize không?
6. Nếu phải multi-region hoặc high-availability, phần nào của current design sẽ đau nhất?

### 6.4 Server + Data phải tự trả lời được

- schema hiện tại phục vụ được demo vì sao
- điểm nào là nợ kỹ thuật dữ liệu
- vì sao lock ở repository/service quan trọng hơn chỉ viết query CRUD đơn thuần
- vì sao ranking và analytics không nên làm naive khi dữ liệu lớn
- vì sao outbox, broker ngoài, migration chuẩn là hướng đi khi hệ thống lớn lên

## 7. Kịch bản hỏi chéo giữa 3 người

### 7.1 Backend hỏi Hardcore

1. Vì sao bid không thể chỉ validate ở controller rồi save luôn?
2. Nếu hai request bid cùng vào trong 1 ms thì state cuối được chốt theo nguyên tắc nào?
3. Anti-sniping cần nằm trong transaction hay chỉ cần update sau?
4. Nếu event đã gửi mà transaction fail thì hậu quả UI là gì?

### 7.2 Hardcore hỏi Server + Data

1. Nếu chạy 2 instance backend thì `ReentrantLock` trong RAM còn tác dụng gì?
2. DB lock hiện tại có đủ để bảo vệ shared state giữa nhiều node không?
3. Bảng bid tăng rất nhanh thì index nào là bắt buộc?
4. Vì sao `double` trong bid là điểm cần cảnh giác ở tầng dữ liệu?

### 7.3 Server + Data hỏi Backend

1. Requirement và API hiện tại chưa trùng hoàn toàn, backend định giữ backward compatibility thế nào?
2. Nếu thêm field mới cho detail/list response thì có làm vỡ UI không?
3. Vì sao một số endpoint chọn DTO riêng thay vì generic response?
4. Nếu client cần polling fallback khi socket lỗi thì backend đã có endpoint snapshot nào để hỗ trợ?

### 7.4 Cả 3 cùng phải thống nhất

1. State chính thức sẽ dùng tên nào: `ACTIVE/FINISHED` hay `OPEN/CLOSED`.
2. Event chính thức sẽ dùng tên nào: `AUCTION_CLOSED` hay `AUCTION_FINISHED`.
3. Favorite API có rename về `saved-auctions` hay không.
4. Auto-bid contract chính thức sẽ là status riêng hay dùng list + current user filtering.
5. Tiền tệ trong bid có giữ `double` cho demo hay đổi sang `BigDecimal`.

## 8. Bộ câu hỏi bảo vệ nhanh 10 phút cuối

1. Hệ thống của nhóm chia module như thế nào?
2. Vì sao chọn REST cho CRUD và WebSocket cho live bidding?
3. Luồng đặt giá từ lúc client bấm nút đến lúc các client khác nhìn thấy giá mới diễn ra thế nào?
4. Shared state của một auction là gì?
5. Vì sao phải lock theo `auctionId`?
6. Vì sao cần bid history bên cạnh current price/current leader?
7. Anti-sniping hiện hoạt động ra sao?
8. Auto-bid hiện dùng rule ưu tiên nào?
9. Hệ thống hiện có những điểm nào mới phù hợp demo chứ chưa production-grade?
10. Nếu có thêm thời gian, 3 cải tiến kỹ thuật ưu tiên nhất của nhóm là gì?

## 9. Gợi ý chia lời khi bảo vệ

### Backend

- kể kiến trúc API, auth, item-admin-auction flow, DTO contract, UI support

### Hardcore

- kể bid flow, transaction, row lock, retry, auto-bid, anti-sniping, after-commit realtime

### Server + Data

- kể schema, repository lock, indexing, money type, outbox, scale, migration, bottleneck dữ liệu

## 10. 5 điểm nhóm nên tự thống nhất trước khi ôn miệng

1. Dùng chung một từ cho state và event, tránh mỗi người nói một kiểu.
2. Thống nhất rõ cái nào là "đã làm", cái nào là "đã thiết kế", cái nào là "định hướng nếu mở rộng".
3. Không né các trade-off như `double`, `SimpleBroker`, `ddl-auto=update`; phải nói thẳng và giải thích vì sao vẫn chấp nhận cho phase hiện tại.
4. Khi bị hỏi sâu, luôn trả lời theo trục `problem -> design choice -> trade-off -> hướng nâng cấp`.
5. Người nào cũng phải hiểu ít nhất một lượt đầy đủ của bid flow, không để chỉ một người biết phần khó nhất.
