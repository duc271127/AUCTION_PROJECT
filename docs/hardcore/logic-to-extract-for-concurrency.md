# Logic To Extract For Future Concurrency Integration

## 1. Mục tiêu tài liệu
Tài liệu này xác định các khối logic nên được tách riêng từ sớm để ở các phase sau có thể gắn concurrency, realtime, auto-bid và anti-sniping vào hệ thống mà không phải đập lại toàn bộ bid flow.

Tài liệu này đặc biệt phục vụ vai trò HARDCORE.

Mục tiêu:
- tách rõ business rule thuần và concurrency control
- chuẩn bị kiến trúc cho `lock per auction`
- tránh viết một hàm `placeBid()` quá lớn và khó mở rộng
- làm nền cho phase 2, phase 3 và phase 4

---

## 2. Vấn đề nếu không tách logic từ sớm
Nếu toàn bộ bid flow bị dồn vào một method hoặc một service lớn, ví dụ:

- validate bid
- lock
- cập nhật current price
- cập nhật leader
- lưu bid history
- xử lý auto-bid
- xử lý anti-sniping
- broadcast socket
- log

thì sẽ gặp các vấn đề sau:

- rất khó test từng phần
- rất khó thay đổi lock strategy
- rất khó debug race condition
- rất khó chèn auto-bid hoặc anti-sniping
- code bị dính chặt giữa business logic, concurrency và realtime
- phase sau muốn mở rộng phải sửa lại quá nhiều

Vì vậy, ở góc nhìn HARDCORE, ngay từ đầu cần tách kiến trúc thành các khối rõ ràng.

---

## 3. Nguyên tắc tách logic
Các logic nên được chia theo vai trò kỹ thuật:

### 3.1 Business rule thuần
Là logic nghiệp vụ không cần biết tới thread, socket hay database.

### 3.2 Shared state mutation
Là logic làm thay đổi state dùng chung của auction.

### 3.3 Concurrency control
Là logic chịu trách nhiệm khóa và điều phối các request bid đồng thời.

### 3.4 Realtime event
Là logic tạo payload event và phát event tới client.

### 3.5 Advanced bidding features
Là logic nâng cao như auto-bid, anti-sniping, reconnect.

---

## 4. Các logic nên tách riêng

## 4.1 BidValidator
### Mục đích
Tách riêng phần kiểm tra bid hợp lệ hay không.

### Nên chứa
- auction có đang `OPEN` không
- current time có còn trong thời gian cho phép bid không
- bidder có hợp lệ không
- bidder có phải seller của auction không
- bid amount có lớn hơn current price không
- bid amount có đạt min increment không

### Không nên chứa
- lock
- broadcast socket
- database save trực tiếp
- logic update state

### Vì sao phải tách
`BidValidator` là business rule thuần. Khi phase sau gắn concurrency, bạn chỉ việc gọi validator ở bên trong critical section mà không phải trộn rule với thread control.

### Gợi ý tên class
- `BidValidator`
- `BidValidationService`
- `BidRules`

---

## 4.2 AuctionStateUpdater
### Mục đích
Tách riêng phần cập nhật state của auction sau khi bid hợp lệ.

### Nên chứa
- cập nhật `currentPrice`
- cập nhật `currentLeader`
- thêm `bidHistory`
- đổi `auctionStatus` nếu cần
- cập nhật `endTime` nếu anti-sniping
- chuẩn bị state mới của auction

### Không nên chứa
- lock manager
- socket broadcast trực tiếp
- parse request
- format response cho controller

### Vì sao phải tách
Đây là phần động vào shared state. Ở phase sau, HARDCORE cần bảo vệ chính xác khối update này bằng lock per auction.

### Gợi ý tên class
- `AuctionStateUpdater`
- `AuctionStateService`
- `BidStateTransitionService`

---

## 4.3 AuctionLockManager
### Mục đích
Tách riêng phần quản lý lock theo auction.

### Nên chứa
- map giữa `auctionId` và `Lock`
- lấy lock tương ứng với auction
- tái sử dụng lock cho cùng một auction
- đảm bảo mỗi auction có đúng một lock logic tại một thời điểm

### Không nên chứa
- business rule của bid
- logic update giá
- logic socket
- logic UI

### Vì sao phải tách
Đây là lõi concurrency của HARDCORE. Nếu nhúng lock trực tiếp vào service bid theo kiểu ad-hoc, code sẽ rất khó mở rộng.

### Gợi ý tên class
- `AuctionLockManager`
- `LockRegistry`
- `AuctionMutexManager`

### Ý tưởng sử dụng
```java
Lock lock = auctionLockManager.getLock(auctionId);
lock.lock();
try {
    // xử lý bid
} finally {
    lock.unlock();
}
```

---

## 4.4 ConcurrentBidProcessor
### Mục đích
Đây là class điều phối bid flow theo cách thread-safe.

### Nên chứa
Luồng xử lý tổng quát:
1. nhận auctionId, bidder, bidAmount
2. lấy lock theo auction
3. đọc state mới nhất
4. gọi validator
5. gọi state updater
6. tạo kết quả xử lý
7. trả về payload cần cho các bước tiếp theo

### Không nên chứa
- parse HTTP request
- render response cho UI
- thao tác giao diện
- logic CRUD chung không liên quan bid

### Vì sao phải tách
Đây là chỗ phase 2–3 sẽ gắn:
- concurrency thật
- timestamp ordering
- test concurrent
- auto-bid
- anti-sniping
- realtime event production

### Gợi ý tên class
- `ConcurrentBidProcessor`
- `ThreadSafeBidService`
- `BidExecutionCoordinator`

---

## 4.5 BidProcessingResult
### Mục đích
Tách riêng object kết quả sau khi xử lý bid.

### Nên chứa
- bid accepted hay rejected
- message
- current price mới
- current leader mới
- auction status mới
- bid history item mới
- flag anti-sniping có xảy ra không
- danh sách event cần phát

### Không nên chứa
- logic xử lý
- database access
- thread control

### Vì sao phải tách
Nếu không có object kết quả rõ ràng, controller và realtime layer sẽ rất khó phối hợp. Ngoài ra phase sau rất khó test concurrent scenarios.

### Gợi ý tên class
- `BidProcessingResult`
- `BidExecutionResult`
- `BidDecisionResult`

---

## 4.6 RealtimeEventFactory
### Mục đích
Tách phần tạo event realtime ra khỏi business logic.

### Nên chứa
- build `BidPlaced`
- build `LeaderChanged`
- build `AuctionFinished`
- build `BidRejected`
- build `AuctionExtended`

### Không nên chứa
- gửi socket trực tiếp
- lock
- validate bid
- update database

### Vì sao phải tách
Event là một lớp riêng. State đã chốt xong rồi thì factory chỉ việc dựng payload event đúng cấu trúc cho UI và socket layer.

### Gợi ý tên class
- `RealtimeEventFactory`
- `AuctionEventFactory`
- `BidEventMapper`

---

## 4.7 RealtimeNotifier
### Mục đích
Tách phần phát event tới client.

### Nên chứa
- broadcast theo `auctionId`
- gửi event tới room/channel đúng auction
- phân phối event tới các client đang theo dõi

### Không nên chứa
- logic validate bid
- update state auction
- business rule
- lock

### Vì sao phải tách
Thao tác gửi socket thường nên nằm ngoài lock. Tách riêng notifier giúp bạn dễ đảm bảo:
- state được chốt trước
- event được gửi sau
- lock không bị giữ quá lâu

### Gợi ý tên class
- `RealtimeNotifier`
- `SocketBroadcastService`
- `AuctionRealtimePublisher`

---

## 4.8 AuctionCloseService
### Mục đích
Tách logic đóng auction khỏi logic bid thường.

### Nên chứa
- kiểm tra auction đã đến giờ đóng chưa
- chốt winner
- chốt final price
- cập nhật `auctionStatus = CLOSED`
- tạo event `AuctionFinished`

### Không nên chứa
- logic CRUD user/item
- logic UI
- parse request

### Vì sao phải tách
Về sau sẽ có case:
- một thread đang close auction
- thread khác đang xử lý bid
Nếu close logic không tách rõ, rất khó đảm bảo tính nhất quán winner.

### Gợi ý tên class
- `AuctionCloseService`
- `AuctionFinalizationService`
- `WinnerResolver`

---

## 4.9 AutoBidEngine
### Mục đích
Tách logic auto-bid khỏi bid flow cơ bản.

### Nên chứa
- lưu và xử lý max bid
- xác định khi nào auto-bid kích hoạt
- tính mức raise tiếp theo
- xác định ai vẫn là leader khi có cạnh tranh

### Không nên chứa
- socket trực tiếp
- controller logic
- validation format request

### Vì sao phải tách
Auto-bid là phần rất dễ làm bid flow bị rối. Nếu không tách riêng từ đầu, phase 3–4 sẽ rất khó mở rộng.

### Gợi ý tên class
- `AutoBidEngine`
- `AutoBidService`
- `AutoBidResolver`

---

## 4.10 AntiSnipingService
### Mục đích
Tách logic gia hạn auction khỏi bid flow cơ bản.

### Nên chứa
- kiểm tra bid có đến quá sát giờ đóng không
- tính end time mới
- xác định có cần phát event `AuctionExtended` không

### Không nên chứa
- validate user
- CRUD item
- logic UI

### Vì sao phải tách
Anti-sniping là advanced rule. Nó nên là một khối riêng để có thể bật/tắt hoặc thay đổi rule dễ dàng.

### Gợi ý tên class
- `AntiSnipingService`
- `AuctionExtensionPolicy`
- `AuctionTimeExtensionService`

---

## 5. Tổ chức logic theo tầng

## 5.1 Business Rule Layer
Không biết gì về thread, socket, UI.

Bao gồm:
- `BidValidator`
- `WinnerResolver`
- `AutoBidEngine`
- `AuctionExtensionPolicy`

## 5.2 State Mutation Layer
Chịu trách nhiệm đổi shared state của auction.

Bao gồm:
- `AuctionStateUpdater`
- `BidHistoryAppender`

## 5.3 Concurrency Layer
Chịu trách nhiệm lock và điều phối xử lý đồng thời.

Bao gồm:
- `AuctionLockManager`
- `ConcurrentBidProcessor`

## 5.4 Realtime Layer
Tạo và phát event sau khi state đã chốt.

Bao gồm:
- `RealtimeEventFactory`
- `RealtimeNotifier`

## 5.5 API Layer
Nhận request từ REST hoặc socket handler rồi gọi processor.

Bao gồm:
- `BidController`
- `AuctionSocketHandler`

---

## 6. Flow kiến trúc đẹp để gắn concurrency sau này

```text
BidController
   -> ConcurrentBidProcessor
      -> AuctionLockManager
      -> BidValidator
      -> AuctionStateUpdater
      -> AutoBidEngine (phase sau)
      -> AntiSnipingService (phase sau)
      -> RealtimeEventFactory
   -> RealtimeNotifier
```

### Ý nghĩa
- `BidController` chỉ nhận request và gọi service
- `ConcurrentBidProcessor` điều phối luồng xử lý thread-safe
- `AuctionLockManager` chịu trách nhiệm lock per auction
- `BidValidator` kiểm tra rule
- `AuctionStateUpdater` đổi state
- `RealtimeEventFactory` tạo event
- `RealtimeNotifier` gửi event ra client

---

## 7. Những gì không nên trộn trong một method duy nhất
Không nên viết kiểu:

```java
public void placeBid(...) {
    // parse request
    // validate
    // synchronized / lock
    // update current price
    // update leader
    // save db
    // auto bid
    // anti sniping
    // broadcast socket
    // logging
}
```

### Vì sao đây là thiết kế xấu
- khó test
- khó tách concurrency
- khó chèn auto-bid
- khó chèn anti-sniping
- khó debug race condition
- rất khó thay đổi sau này

---

## 8. Đề xuất cấu trúc package cho HARDCORE

```text
backend/src/main/java/com/team/backend
├─ concurrent
│  ├─ AuctionLockManager.java
│  ├─ ConcurrentBidProcessor.java
│  └─ BidLockPrototype.java
├─ bidding
│  ├─ BidValidator.java
│  ├─ AuctionStateUpdater.java
│  ├─ BidProcessingResult.java
│  └─ WinnerResolver.java
├─ realtime
│  ├─ RealtimeEventFactory.java
│  └─ RealtimeNotifier.java
├─ autobid
│  └─ AutoBidEngine.java
├─ extension
│  └─ AntiSnipingService.java
```

---

## 9. Những logic nên ưu tiên tách ngay sau Phase 1
Nếu chuẩn bị sang Phase 2, HARDCORE nên ưu tiên tách tối thiểu 3 logic sau:

### 9.1 AuctionLockManager
Đây là xương sống cho lock per auction.

### 9.2 ConcurrentBidProcessor
Đây là nơi orchestration của bid flow thread-safe.

### 9.3 BidProcessingResult
Đây là object trung gian giúp controller, realtime và test giao tiếp rõ ràng.

### Vì sao ưu tiên 3 logic này
Vì đây là bộ khung tối thiểu để:
- chuyển từ prototype sang processor thật
- cắm lock strategy rõ ràng
- chuẩn bị concurrent test
- nối tiếp sang event flow phase sau

---

## 10. Kết luận
Các logic nên được tách riêng để sau này gắn concurrency gồm:

### Bắt buộc tách
- `BidValidator`
- `AuctionStateUpdater`
- `AuctionLockManager`
- `ConcurrentBidProcessor`
- `BidProcessingResult`
- `RealtimeEventFactory`
- `RealtimeNotifier`

### Nên tách thêm cho phase sau
- `AuctionCloseService`
- `AutoBidEngine`
- `AntiSnipingService`

Nguyên tắc cốt lõi là:
- business rule phải tách khỏi thread control
- shared state mutation phải rõ ràng
- realtime phải tách khỏi bid core logic
- advanced feature phải có chỗ cắm riêng

Nếu tách đúng từ bây giờ, phase 2 và phase 3 sẽ chỉ là mở rộng dần, không phải đập lại toàn bộ bid flow.
