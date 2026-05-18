# Bid Flow — Outside Lock / Inside Lock / After Lock

## 1. Mục tiêu tài liệu
Tài liệu này mô tả cách chia luồng xử lý bid thành 3 vùng rõ ràng:

- **Outside Lock**
- **Inside Lock**
- **After Lock**

Mục tiêu là để bid flow của hệ thống đủ sạch cho việc gắn concurrency thật ở các phase sau, đặc biệt là:

- lock per auction
- concurrent bidding
- realtime event
- auto-bid
- anti-sniping

---

## 2. Vì sao phải chia bid flow thành 3 vùng
Nếu toàn bộ xử lý bid bị dồn vào một method lớn, ví dụ:

- parse request
- validate input
- lock
- đọc current price
- validate bid
- update current price
- update leader
- lưu bid history
- build event
- broadcast socket
- log

thì sẽ gặp các vấn đề sau:

- khó test từng phần
- khó debug race condition
- khó thay đổi lock strategy
- khó thêm auto-bid và anti-sniping
- dễ giữ lock quá lâu
- làm giảm hiệu năng hệ thống

Vì vậy, bid flow nên được chia thành 3 vùng kỹ thuật rõ ràng.

---

## 3. Tổng quan 3 vùng của bid flow

### 3.1 Outside Lock
Đây là phần xử lý có thể làm trước khi vào vùng critical section.

### 3.2 Inside Lock
Đây là phần xử lý bắt buộc phải thread-safe, vì có đọc/ghi shared state của auction.

### 3.3 After Lock
Đây là phần xử lý sau khi state đã được chốt xong và lock đã được release.

---

## 4. Outside Lock — Những gì nên làm ngoài lock

## 4.1 Nhận request bid
Ví dụ:
- nhận HTTP request từ controller
- nhận message từ socket handler
- nhận dữ liệu từ UI/client

### Lý do
Ở bước này hệ thống mới chỉ nhận input, chưa động vào shared state của auction.

---

## 4.2 Parse request
Ví dụ:
- map request body sang DTO
- đọc `auctionId`
- đọc `bidderId`
- đọc `bidAmount`

### Lý do
Đây là xử lý cục bộ của request, không cần khóa auction.

---

## 4.3 Validate format input
Ví dụ:
- `bidAmount` có null không
- `bidAmount` có đúng kiểu số không
- `auctionId` có hợp lệ không

### Lý do
Đây là validation mức request format, chưa cần đọc state dùng chung của auction.

---

## 4.4 Xác thực request cơ bản
Ví dụ:
- user đã đăng nhập chưa
- token có hợp lệ không
- user có tồn tại không

### Lý do
Đây là bước bảo mật và xác thực cơ bản, không nên đặt trong lock để tránh giữ lock không cần thiết.

---

## 4.5 Chuẩn bị context xử lý
Ví dụ:
- tạo object request nội bộ
- gom các thông tin đầu vào
- chuẩn bị log đầu vào

### Lý do
Đây vẫn là bước chuẩn bị, chưa cần vào vùng thread-safe.

---

## 5. Inside Lock — Những gì bắt buộc phải nằm trong lock

Đây là phần quan trọng nhất của bid flow.

Inside lock phải gói toàn bộ đoạn:
- đọc shared state hiện tại
- validate nghiệp vụ theo state mới nhất
- cập nhật shared state
- chốt payload event

Nguyên tắc:
> Nếu hai request cùng chạy ở đoạn này mà có thể làm sai current price, leader, status hoặc history thì đoạn đó phải nằm trong lock.

---

## 5.1 Đọc state hiện tại của auction
Bao gồm:
- `auctionStatus`
- `currentPrice`
- `currentLeader`
- `endTime` nếu có anti-sniping

### Vì sao phải trong lock
Nếu đọc outside lock:
- request A và request B có thể cùng đọc state cũ
- một request sẽ ra quyết định trên dữ liệu không còn mới nhất

---

## 5.2 Validate bid theo state hiện tại
Bao gồm:
- auction có đang `OPEN` không
- bid có lớn hơn current price không
- bid có đạt min increment không
- user có phải seller không
- bid có đến quá muộn không

### Vì sao phải trong lock
Các rule này phụ thuộc vào state hiện tại của auction. Nếu validate ngoài lock, kết quả có thể đã lỗi thời khi update xảy ra.

---

## 5.3 Cập nhật current price
### Vì sao phải trong lock
`currentPrice` là shared state quan trọng nhất. Nếu nhiều thread cùng ghi mà không có lock:
- có thể mất update
- giá cuối không phản ánh đúng thứ tự xử lý
- realtime event có thể sai

---

## 5.4 Cập nhật current leader
### Vì sao phải trong lock
`currentLeader` phải luôn khớp với `currentPrice`.

Không được để trạng thái:
- `currentPrice = 130`
- nhưng `currentLeader = UserA`
trong khi đúng ra là `UserB`

---

## 5.5 Ghi bid history
### Vì sao phải trong lock
Bid history phải nhất quán với:
- current price mới
- leader mới
- thứ tự xử lý bid

Nếu state đã đổi mà history chưa đồng bộ:
- audit sẽ khó giải thích
- UI realtime có thể lệch

---

## 5.6 Xử lý anti-sniping nếu có
Ví dụ:
- nếu bid đến trong 30 giây cuối
- thì gia hạn end time thêm 2 phút

### Vì sao phải trong lock
Nhiều bid sát giờ có thể cùng kích hoạt logic gia hạn. Nếu không khóa:
- `endTime` có thể bị ghi đè sai
- extension có thể chạy nhiều lần ngoài ý muốn

---

## 5.7 Cập nhật auction status nếu cần
Ví dụ:
- `OPEN -> EXTENDED`
- `OPEN -> CLOSED`

### Vì sao phải trong lock
Status của auction liên quan trực tiếp tới việc có được nhận bid tiếp hay không.

---

## 5.8 Chuẩn bị payload event
Ví dụ build:
- `BidPlaced`
- `LeaderChanged`
- `AuctionExtended`
- `AuctionFinished`

### Vì sao nên làm trong lock
Payload event phải phản ánh đúng state cuối đã được chốt.

Lưu ý:
- **build event payload** trong lock
- **broadcast event** sau lock

---

## 6. After Lock — Những gì nên làm sau khi release lock

## 6.1 Broadcast realtime event
Ví dụ:
- gửi socket event tới room theo `auctionId`
- push event tới client đang xem auction

### Vì sao nên để sau lock
Gửi event ra ngoài có thể chậm hơn dự kiến. Nếu giữ lock trong lúc broadcast:
- lock bị giữ quá lâu
- làm giảm throughput
- bid khác phải chờ lâu hơn

---

## 6.2 Trả response về client
Ví dụ:
- trả `Bid accepted`
- trả `Bid rejected`
- trả current price mới

### Vì sao nên để sau lock
State đã chốt xong rồi, không cần giữ lock thêm nữa.

---

## 6.3 Logging phụ
Ví dụ:
- log debug
- log response
- log thống kê thời gian xử lý

### Vì sao nên để sau lock
Không nên giữ lock cho những việc không làm thay đổi shared state.

---

## 6.4 Trigger các xử lý phụ không đổi state chính
Ví dụ:
- gửi notification không quan trọng
- analytics
- metrics

### Vì sao nên để sau lock
Các xử lý phụ này không nên cản bid flow chính.

---

## 7. Flow chuẩn đề xuất

```text
Receive Bid Request
    |
    |-- Parse request
    |-- Validate input format
    |-- Authenticate basic user info
    |
Acquire lock by auctionId
    |
    |-- Read current auction state
    |-- Validate business rules
    |-- Update currentPrice
    |-- Update currentLeader
    |-- Save bidHistory
    |-- Update endTime if anti-sniping
    |-- Update auctionStatus if needed
    |-- Build realtime event payload
Release lock
    |
    |-- Broadcast event
    |-- Return response
    |-- Write auxiliary logs
```

---

## 8. Ví dụ minh họa

## 8.1 Ví dụ đúng
- current price = 100
- user A bid 120
- user B bid 130

### Luồng đúng
- A vào lock trước
- A đọc giá 100, validate, cập nhật lên 120, leader = A
- A build payload event rồi release lock
- B vào sau
- B đọc giá mới là 120, validate, cập nhật lên 130, leader = B
- B build payload event rồi release lock
- hệ thống broadcast event tương ứng

### Kết quả
- final current price = 130
- final leader = UserB

---

## 8.2 Ví dụ sai nếu validate ngoài lock
- current price = 100
- A và B cùng đọc 100 bên ngoài lock
- A định bid 120
- B định bid 110

Nếu validate ở ngoài lock:
- A thấy hợp lệ
- B cũng thấy hợp lệ
- nhưng sau khi A update xong, B thực ra không còn hợp lệ nữa

### Kết luận
Validate theo shared state không được để outside lock.

---

## 9. Quy tắc thiết kế quan trọng

## 9.1 Không giữ lock lâu hơn cần thiết
Chỉ giữ lock cho:
- đọc state
- validate theo state
- update state
- build payload event

Không giữ lock cho:
- parse request
- broadcast socket
- log phụ

---

## 9.2 Không để shared state bị cập nhật rời rạc
Các phần sau phải đi cùng nhau trong inside lock:
- `currentPrice`
- `currentLeader`
- `bidHistory`
- `auctionStatus`
- `endTime` nếu có

---

## 9.3 Không trộn business rule và broadcast
Business rule phải chạy xong, state phải chốt xong rồi mới broadcast.

---

## 10. Kết luận
Bid flow nên được chia thành 3 vùng rõ ràng:

### Outside Lock
- nhận request
- parse request
- validate format
- xác thực user cơ bản

### Inside Lock
- đọc state hiện tại
- validate theo state hiện tại
- update current price
- update current leader
- ghi bid history
- cập nhật status/end time
- build payload event

### After Lock
- broadcast event
- trả response
- log phụ

Nguyên tắc cốt lõi là:

> Chỉ những đoạn nào thật sự đụng tới shared state của auction mới được giữ trong lock. Những việc còn lại nên đẩy ra ngoài lock để bid flow vừa đúng vừa hiệu quả.
