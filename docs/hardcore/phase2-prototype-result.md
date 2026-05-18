# Phase 2 Prototype Result - HARDCORE

## 1. Mục tiêu
Tài liệu này ghi lại kết quả prototype của HARDCORE trong Phase 2, tập trung vào:

- lock theo auction bản đầu
- xử lý bid đồng thời bằng `ConcurrentBidProcessor`
- test hai request/bid đến gần nhau
- kiểm tra final state của auction có nhất quán hay không

---

## 2. Thành phần đã dùng trong prototype
Prototype Phase 2 sử dụng các thành phần sau:

- `AuctionState`
- `AuctionLockManager`
- `ConcurrentBidProcessor`
- `BidProcessingResult`
- `BidLockPrototype`

Các thành phần này cho phép mô phỏng luồng:
- nhận bid
- lấy lock theo `auctionId`
- validate theo state hiện tại
- cập nhật current price
- cập nhật current leader
- trả kết quả xử lý

---

## 3. Mục tiêu kiểm tra
Prototype Phase 2 cần chứng minh được các điểm sau:

- bid cùng một auction không được cập nhật state tự do
- lock theo auction đang hoạt động
- thread vào sau phải xử lý trên state mới nhất
- final current price phải đúng
- final current leader phải đúng
- bid không hợp lệ phải bị reject đúng

---

## 4. Kịch bản test đã thực hiện

## 4.1 Kịch bản 1 — Hai bid đều hợp lệ
### Input
- auctionId = 1
- current price ban đầu = 100
- auction status = OPEN
- UserA bid 120
- UserB bid 130

### Kỳ vọng
- UserA được accept nếu vào lock trước
- UserB tiếp tục vào lock sau
- UserB đọc state mới nhất sau khi UserA cập nhật
- final state:
  - `currentPrice = 130`
  - `currentLeader = UserB`

### Kết quả
- hai thread không cùng sửa state một lúc
- state cuối cùng đúng
- không có lost update
- leader cuối đúng là user có mức bid cao hơn

### Kết luận
Kịch bản này chứng minh lock per auction đang hoạt động đúng với trường hợp 2 bid hợp lệ gần nhau.

---

## 4.2 Kịch bản 2 — Bid sau thấp hơn giá mới
### Input
- auctionId = 1
- current price ban đầu = 100
- auction status = OPEN
- UserA bid 130
- UserB bid 120

### Kỳ vọng
- UserA được accept trước
- current price tăng lên 130
- UserB vào sau phải nhìn thấy current price mới là 130
- bid 120 của UserB phải bị reject
- final state:
  - `currentPrice = 130`
  - `currentLeader = UserA`

### Kết quả
- request vào sau không validate trên giá cũ
- request sau bị reject đúng theo current price mới nhất
- state cuối không bị rollback

### Kết luận
Kịch bản này chứng minh validation theo state hiện tại đang được thực hiện đúng bên trong lock.

---

## 4.3 Kịch bản 3 — Auction đã đóng
### Input
- auctionId = 1
- current price ban đầu = 100
- auction status = CLOSED
- UserA bid 120
- UserB bid 130

### Kỳ vọng
- cả hai bid đều bị reject
- current price không đổi
- current leader không đổi

### Kết quả
- bid không được nhận khi auction đã CLOSED
- state auction giữ nguyên

### Kết luận
Kịch bản này chứng minh rule `auction must be OPEN to accept bid` đang được xử lý đúng trong processor.

---

## 5. Phân tích kỹ thuật

## 5.1 Điều gì chứng minh lock đang hoạt động
Dựa trên log chạy prototype:
- thread đầu vào trước sẽ acquire lock trước
- thread sau chỉ xử lý sau khi thread trước release lock
- bid sau luôn nhìn thấy state mới nhất của auction

Điều này giúp:
- tránh race condition
- tránh lost update
- đảm bảo final state nhất quán

---

## 5.2 Vì sao final state đúng
Final state đúng vì:
- update `currentPrice` và `currentLeader` được làm trong cùng critical section
- validate bid dựa trên state mới nhất chứ không dựa trên state cũ
- lock chỉ áp dụng cho từng auction nên không chặn các auction khác không liên quan

---

## 5.3 Những gì prototype Phase 2 chưa làm
Prototype Phase 2 mới chỉ là bản nền cho concurrency, chưa bao gồm:

- realtime socket thật
- broadcast event thật
- auto-bid
- anti-sniping
- persistence vào database thật
- close auction song song với bid thật

Các phần này sẽ được gắn ở phase sau.

---

## 6. Ý nghĩa của prototype đối với Phase 3
Prototype Phase 2 giúp nhóm có nền để bước sang Phase 3:

- đã có `lock per auction`
- đã có processor thread-safe bản đầu
- đã có object kết quả xử lý bid
- đã có cấu trúc để gắn realtime event và advanced rule

Nói cách khác, Phase 2 đã chuyển hệ thống từ mức “ý tưởng concurrency” sang mức “khung xử lý concurrency có thể mở rộng”.

---

## 7. Kết luận
Prototype Phase 2 đã đạt được các mục tiêu chính:

- chứng minh được `lock per auction`
- chứng minh được bid gần nhau vẫn cho final state đúng
- chứng minh validation phải đi theo state hiện tại trong lock
- chuẩn bị khung kỹ thuật cho concurrent bidding thật ở phase sau

Đây là deliverable quan trọng của HARDCORE trong Phase 2.
