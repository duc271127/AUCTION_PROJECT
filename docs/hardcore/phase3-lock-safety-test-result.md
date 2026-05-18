# Phase 3 Lock Safety Test Result - HARDCORE

## 1. Mục tiêu
Tài liệu này ghi lại kết quả test cho phần lock safety trong Phase 3 của HARDCORE, nhằm kiểm tra rằng bid flow thật đã xử lý đúng các tình huống đồng thời quan trọng.

Các mục tiêu cần kiểm tra gồm:

- chống lost update
- chống rollback sai
- chống trường hợp 2 người cùng thắng
- đảm bảo lock theo `auctionId` đang hoạt động đúng trong bid flow thật

---

## 2. Thành phần đã dùng để test
Phần test được thực hiện dựa trên các thành phần sau:

- `AuctionLockManager`
- `ConcurrentBidProcessor`
- `AuctionState`
- `BidRecord`
- `BidController`
- `AuctionRegistry`
- Postman để gửi request thật qua REST API

Endpoint đã dùng:

- `GET /api/auctions/{auctionId}`
- `POST /api/auctions/{auctionId}/bids`
- `POST /api/auctions/{auctionId}/close`

---

## 3. Kịch bản test 1 — Chống lost update

### Mục tiêu
Kiểm tra rằng khi hai request bid đến gần nhau, hệ thống không làm mất lần cập nhật hợp lệ nào.

### Input
- auctionId = 1
- current price ban đầu = 100
- auction status = OPEN
- request 1:
  - bidderName = UserA
  - bidAmount = 120
- request 2:
  - bidderName = UserB
  - bidAmount = 130

### Kỳ vọng
- request đầu hợp lệ sẽ cập nhật current price
- request sau phải nhìn thấy state mới nhất
- final state phải là:
  - `currentPrice = 130`
  - `currentLeader = UserB`

### Kết quả thực tế
- request của UserA được accept
- request của UserB được accept
- sau khi kiểm tra lại auction:
  - current price là 130
  - current leader là UserB
- không có lần cập nhật hợp lệ nào bị mất

### Kết luận
Lock theo auction đang giúp các request bid vào cùng auction được xử lý tuần tự, nên không xảy ra lost update.

---

## 4. Kịch bản test 2 — Chống rollback sai

### Mục tiêu
Kiểm tra rằng request đến sau nhưng bid thấp hơn không được phép ghi đè state mới.

### Input
- auctionId = 1
- current price ban đầu = 100
- auction status = OPEN
- request 1:
  - bidderName = UserA
  - bidAmount = 130
- request 2:
  - bidderName = UserB
  - bidAmount = 120

### Kỳ vọng
- request của UserA được accept
- current price tăng lên 130
- request của UserB khi vào sau phải nhìn thấy current price mới là 130
- bid 120 phải bị reject
- final state phải là:
  - `currentPrice = 130`
  - `currentLeader = UserA`

### Kết quả thực tế
- request của UserA được accept
- request của UserB bị reject với message:
  - `"Bid amount must be greater than current price"`
- current price sau cùng vẫn là 130
- current leader sau cùng vẫn là UserA

### Kết luận
Validation đang được thực hiện theo state mới nhất bên trong lock, nên không xảy ra rollback sai hoặc ghi đè state mới bằng dữ liệu cũ.

---

## 5. Kịch bản test 3 — Chống 2 người cùng thắng

### Mục tiêu
Kiểm tra rằng sau khi auction được close, chỉ có duy nhất một winner.

### Input
- auctionId = 1
- auction đang OPEN
- có các bid hợp lệ trước khi close
- gọi:
  - `POST /api/auctions/1/close`

### Kỳ vọng
- auction chuyển sang `CLOSED`
- winner phải là `currentLeader` cuối cùng tại thời điểm close
- không có bid nào sau khi close được accept
- không có tình huống hai winner khác nhau

### Kết quả thực tế
- request close auction thành công
- status của auction chuyển thành `CLOSED`
- winner được gán từ current leader cuối cùng
- khi gửi bid sau khi close:
  - request bị reject
  - state không đổi

### Kết luận
`closeAuction()` đã đi qua cùng lock của auction nên trạng thái đóng phiên và winner được chốt nhất quán. Hệ thống không xuất hiện trường hợp hai người cùng thắng.

---

## 6. Phân tích kỹ thuật

## 6.1 Điều gì chứng minh lock đang hoạt động đúng
Lock hoạt động đúng vì:

- request bid cùng một auction đi qua cùng một `Lock`
- request vào sau chỉ được xử lý sau khi request trước release lock
- validation theo state hiện tại được thực hiện bên trong critical section
- cập nhật `currentPrice`, `currentLeader`, `bidHistory` được thực hiện cùng nhau

---

## 6.2 Điều gì đã được kiểm chứng qua flow thật
Khác với phase trước chỉ dùng prototype, phase này đã test qua REST API thật:

- client gửi request thật tới controller
- controller gọi `ConcurrentBidProcessor`
- processor lấy lock theo `auctionId`
- state auction được cập nhật trong backend đang chạy
- kết quả được kiểm tra lại qua API

Điều này chứng minh lock không chỉ chạy trên demo class mà đã được gắn vào bid flow thật của backend.

---

## 6.3 Các giới hạn hiện tại
Tại thời điểm test này, hệ thống vẫn đang ở mức nền cho concurrent bidding. Chưa bao gồm:

- socket realtime thật
- thread-safe broadcast thật
- auto-bid
- anti-sniping
- stress test số lượng request lớn
- test 2–3 client đồng thời thật sự ở UI layer

Các phần này thuộc bước tiếp theo của Phase 3.

---

## 7. Kết luận tổng
Qua các kịch bản đã test, phần HARDCORE của Phase 3 hiện tại đã chứng minh được:

- lock per auction hoạt động đúng
- bid flow thật đã dùng `ConcurrentBidProcessor`
- không có lost update trong các case đã kiểm tra
- không có rollback sai trong các case đã kiểm tra
- chỉ có một winner khi close auction
- bid sau khi auction đóng bị reject đúng

Đây là nền tảng để tiếp tục sang các task tiếp theo của Phase 3:

- socket server / broadcast event
- thread-safe notify
- stress test concurrent bids
- test 2–3 client cùng lúc
