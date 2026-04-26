# Auction Lock Manager Design

## 1. Mục tiêu tài liệu
Tài liệu này mô tả thiết kế của `AuctionLockManager` trong hệ thống đấu giá, nhằm phục vụ cho xử lý concurrent bidding theo đúng vai trò HARDCORE ở Phase 2.

Mục tiêu:
- thiết kế cơ chế lock theo từng auction
- xác định lock được lấy ở đâu và release ở đâu
- xác định phần nào nằm trong critical section
- làm nền cho phase 3 khi gắn realtime và concurrent bidding thật

---

## 2. Bài toán cần giải quyết
Trong hệ thống đấu giá, nhiều user có thể cùng bid vào một auction gần như đồng thời.

Ví dụ:
- current price = 100
- user A bid 120
- user B bid 130
- hai request đến gần như cùng lúc

Nếu không có cơ chế khóa phù hợp:
- cả hai request có thể cùng đọc state cũ
- current price có thể bị ghi đè sai
- current leader có thể bị sai
- bid history có thể không nhất quán
- winner cuối cùng có thể sai

Do đó cần có một cơ chế điều phối để:
- bid cùng một auction được xử lý tuần tự
- bid ở auction khác nhau vẫn xử lý song song

---

## 3. Quyết định thiết kế
### 3.1 Dùng lock theo auction
Mỗi auction có một lock riêng.

Điều này có nghĩa:
- bid vào cùng một auction sẽ phải dùng chung một lock
- bid vào auction khác nhau sẽ không chặn lẫn nhau

### 3.2 Không dùng global lock
Không nên dùng một lock cho toàn bộ hệ thống vì:
- mọi auction sẽ chặn nhau dù không liên quan
- hệ thống mất khả năng xử lý song song
- hiệu năng giảm không cần thiết

### 3.3 Lock được tra theo `auctionId`
`auctionId` là khóa tự nhiên để xác định shared state của từng phiên đấu giá. Vì vậy lock nên được quản lý theo `auctionId`.

---

## 4. Vai trò của AuctionLockManager
`AuctionLockManager` có nhiệm vụ:

- quản lý tập lock của toàn hệ thống bidding
- trả về đúng lock tương ứng với một `auctionId`
- tái sử dụng cùng một lock cho cùng một auction
- tạo lock mới nếu auction chưa có lock
- giúp `ConcurrentBidProcessor` không phải tự quản lý map lock

Nó **không** nên chứa:
- business rule
- logic validate bid
- logic update current price
- logic socket/realtime
- logic UI

---

## 5. Thiết kế class đề xuất

## 5.1 Nhiệm vụ class
Class nên rất đơn giản:
- chứa `ConcurrentHashMap<Long, Lock>`
- có method `getLock(Long auctionId)`
- dùng `computeIfAbsent` để tạo lock khi cần

## 5.2 Code mẫu
```java
package com.team.backend.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionLockManager {

    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();

    public Lock getLock(Long auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }
}
```

---

## 6. Lock được lấy ở đâu
Lock nên được lấy tại lớp điều phối bid flow thread-safe, ví dụ:

- `ConcurrentBidProcessor`
- `ThreadSafeBidService`

### Vì sao
Vì lock là concern của concurrency orchestration, không phải concern của:
- controller
- validator
- realtime notifier

### Kết luận
Lock **không nên** lấy ở controller và cũng **không nên** nhúng rải rác vào nhiều service khác nhau.

---

## 7. Lock được release ở đâu
Lock phải được release trong khối `finally`.

### Ví dụ
```java
Lock lock = auctionLockManager.getLock(auctionId);
lock.lock();
try {
    // xử lý bid
} finally {
    lock.unlock();
}
```

### Vì sao bắt buộc dùng finally
Nếu trong lúc xử lý bid có exception mà không unlock:
- lock sẽ bị giữ lại
- các request sau vào cùng auction có thể bị treo
- hệ thống dễ bị deadlock cục bộ

### Kết luận
`unlock()` luôn phải đặt trong `finally`.

---

## 8. Vùng critical section gồm những gì
Sau khi lock theo `auctionId`, các bước sau phải nằm trong critical section:

- đọc `auctionStatus`
- đọc `currentPrice`
- đọc `currentLeader`
- đọc `endTime` nếu có anti-sniping
- validate bid theo state hiện tại
- cập nhật `currentPrice`
- cập nhật `currentLeader`
- thêm `bidHistory`
- cập nhật `endTime` nếu anti-sniping
- cập nhật `auctionStatus` nếu cần
- build payload event

### Không nên để trong critical section
- parse request
- validate format input
- xác thực user cơ bản
- broadcast socket thật
- log phụ
- analytics / metrics

### Nguyên tắc
Chỉ giữ lock cho những đoạn thật sự đụng tới shared state của auction.

---

## 9. Luồng xử lý đề xuất với AuctionLockManager

```text
BidController
   -> ConcurrentBidProcessor
      -> AuctionLockManager.getLock(auctionId)
      -> lock.lock()
      -> validate business rules theo state mới nhất
      -> update state auction
      -> build event payload
      -> unlock()
   -> RealtimeNotifier.broadcast(...)
```

---

## 10. Tại sao AuctionLockManager phải tách riêng
Nếu không tách riêng lock manager mà nhúng lock trực tiếp vào `BidService`, về sau sẽ gặp các vấn đề:

- khó thay đổi chiến lược lock
- khó test concurrency riêng
- khó tái sử dụng cho auto-bid, anti-sniping, close auction
- dễ viết lock không đồng nhất giữa nhiều luồng xử lý
- khó giải thích kiến trúc với nhóm và giảng viên

Tách riêng `AuctionLockManager` giúp:
- kiến trúc rõ ràng
- dễ refactor
- dễ mở rộng
- đúng vai trò HARDCORE

---

## 11. Các tình huống mà lock per auction giải quyết tốt

## 11.1 Hai bid cùng vào một auction
- cùng dùng một lock
- xử lý tuần tự
- state cuối nhất quán

## 11.2 Hai bid vào hai auction khác nhau
- mỗi auction dùng lock khác nhau
- xử lý song song được
- không chặn không cần thiết

## 11.3 Auto-bid trong cùng auction
Sau này nếu có auto-bid:
- logic auto-bid cũng nên đi qua lock của auction đó
- giúp state vẫn nhất quán với bid thường

## 11.4 Close auction và bid gần như cùng lúc
Sau này nếu có thread đóng auction:
- close logic cũng nên dùng lock của auction
- tránh vừa bid vừa close gây sai winner

---

## 12. Những rủi ro cần lưu ý

## 12.1 Lock map tăng mãi
Nếu hệ thống có rất nhiều auction, `lockMap` có thể lớn dần theo thời gian.

### Giai đoạn hiện tại
Với phase 2 prototype, đây chưa phải vấn đề lớn.

### Giai đoạn sau
Có thể cần cân nhắc:
- cleanup lock khi auction đã closed lâu
- cơ chế thu hồi lock không còn dùng

---

## 12.2 Không được lock quá rộng
Nếu trong vùng lock bạn để cả:
- broadcast socket
- log nặng
- query không cần thiết

thì lock sẽ bị giữ quá lâu và làm giảm throughput.

---

## 12.3 Không được dùng nhiều lock chồng chéo không kiểm soát
Phase 2 nên giữ đơn giản:
- một auction -> một lock chính

Không nên quá sớm đưa nhiều lock phụ gây phức tạp.

---

## 13. Mối liên hệ với các class khác

## 13.1 Với ConcurrentBidProcessor
`ConcurrentBidProcessor` là nơi sử dụng `AuctionLockManager`.

## 13.2 Với BidValidator
`BidValidator` không cần biết gì về lock. Nó chỉ kiểm tra rule dựa trên state hiện tại.

## 13.3 Với AuctionStateUpdater
`AuctionStateUpdater` chỉ cập nhật state. Nó giả định rằng caller đã đảm bảo thread-safety trước đó.

## 13.4 Với RealtimeEventFactory
Factory chỉ build event từ state đã chốt.

## 13.5 Với RealtimeNotifier
Notifier chỉ phát event sau khi lock đã được release.

---

## 14. Kết luận
Thiết kế `AuctionLockManager` của hệ thống nên đi theo các nguyên tắc sau:

- mỗi `auctionId` có một lock riêng
- lock được lấy trong `ConcurrentBidProcessor`
- lock được release trong `finally`
- critical section chỉ chứa phần đọc/ghi shared state
- không dùng global lock cho toàn hệ thống
- broadcast và xử lý phụ được làm sau khi release lock

Đây là thiết kế nền rất quan trọng để:
- gắn concurrent bidding thật ở phase 3
- gắn auto-bid
- gắn anti-sniping
- đảm bảo hệ thống vẫn đúng khi nhiều request bid đến gần nhau
