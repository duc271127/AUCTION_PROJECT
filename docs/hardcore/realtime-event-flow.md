# Realtime Event Flow - Auction Project

## 1. Mục tiêu tài liệu
Tài liệu này mô tả luồng sự kiện realtime trong hệ thống đấu giá.

Mục tiêu:
- xác định event nào cần có
- xác định event sinh ra ở bước nào
- xác định client nhận gì và update gì
- làm nền cho phần socket/realtime ở phase sau

---

## 2. Vì sao cần event flow
Trong hệ thống đấu giá, khi một bid mới được chấp nhận, không chỉ một người dùng bị ảnh hưởng.

Những client đang xem cùng auction cần được cập nhật:
- giá hiện tại
- người đang dẫn đầu
- lịch sử bid mới
- thời gian còn lại
- trạng thái auction

Nếu không có event flow rõ ràng:
- server khó biết nên phát gì
- client khó biết phải update gì
- dễ xảy ra tình trạng dữ liệu trên UI không đồng bộ

---

## 3. Nguyên tắc thiết kế event
### 3.1 Event chỉ được phát sau khi state đã hợp lệ
Không phát event trước khi:
- validate bid xong
- cập nhật shared state xong
- lưu dữ liệu xong hoặc chốt logic xong

### 3.2 Event phải đủ dữ liệu để client update UI
Payload nên chứa tối thiểu các field mà client cần hiển thị.

### 3.3 Event phải bám theo business rule
Event không chỉ là thông báo kỹ thuật, mà phải phản ánh trạng thái nghiệp vụ thực sự của auction.

---

## 4. Các event chính
Hệ thống đề xuất tối thiểu các event sau:

- `BidPlaced`
- `LeaderChanged`
- `AuctionFinished`

Có thể mở rộng:
- `BidRejected`
- `AuctionExtended`

---

## 5. Chi tiết từng event

## 5.1 Event: BidPlaced
### Ý nghĩa
Một bid hợp lệ đã được chấp nhận vào hệ thống.

### Khi nào phát
Sau khi:
- lock auction
- validate bid thành công
- cập nhật current price
- lưu bid history

### Ai nhận
- các client đang xem auction đó
- có thể cả user vừa bid

### Client cần update gì
- current price
- bid history
- thông báo có bid mới

### Payload gợi ý
```json
{
  "eventType": "BidPlaced",
  "auctionId": 1,
  "bidId": 101,
  "bidderId": 5,
  "bidAmount": 120.0,
  "currentPrice": 120.0,
  "timestamp": "2026-04-08T21:30:00"
}
```

---

## 5.2 Event: LeaderChanged
### Ý nghĩa
Người dẫn đầu của auction đã thay đổi.

### Khi nào phát
Sau khi hệ thống xác định bid mới làm đổi leader hiện tại.

### Ai nhận
- tất cả client đang xem auction đó

### Client cần update gì
- current leader
- current price
- highlight user đang dẫn đầu
- trạng thái nút bid hoặc thông báo phù hợp

### Payload gợi ý
```json
{
  "eventType": "LeaderChanged",
  "auctionId": 1,
  "leaderId": 5,
  "leaderName": "User A",
  "currentPrice": 120.0,
  "timestamp": "2026-04-08T21:30:00"
}
```

---

## 5.3 Event: AuctionFinished
### Ý nghĩa
Phiên đấu giá đã kết thúc.

### Khi nào phát
Khi:
- hết thời gian
- hệ thống chốt trạng thái closed
- xác định winner cuối cùng

### Ai nhận
- tất cả client đang xem auction đó
- có thể user tham gia auction
- có thể seller

### Client cần update gì
- trạng thái auction sang CLOSED
- dừng form bid
- dừng countdown
- hiển thị winner
- hiển thị thông báo auction đã kết thúc

### Payload gợi ý
```json
{
  "eventType": "AuctionFinished",
  "auctionId": 1,
  "winnerId": 5,
  "winnerName": "User A",
  "finalPrice": 150.0,
  "timestamp": "2026-04-08T21:45:00"
}
```

---

## 5.4 Event: BidRejected
### Ý nghĩa
Bid không hợp lệ và bị từ chối.

### Khi nào phát
Có thể dùng khi:
- bid thấp hơn current price
- auction đã đóng
- bid đến quá muộn

### Ai nhận
- thường chỉ cần trả response lỗi cho user gửi bid
- không nhất thiết broadcast cho mọi client

### Client cần update gì
- hiển thị lỗi cho user
- không đổi current price
- không đổi leader

### Payload gợi ý
```json
{
  "eventType": "BidRejected",
  "auctionId": 1,
  "bidderId": 5,
  "message": "Bid amount must be greater than current price",
  "timestamp": "2026-04-08T21:31:00"
}
```

---

## 5.5 Event: AuctionExtended
### Ý nghĩa
Thời gian đấu giá được gia hạn do anti-sniping.

### Khi nào phát
Khi có bid hợp lệ đến quá gần thời điểm đóng phiên và business rule cho phép gia hạn.

### Ai nhận
- tất cả client đang xem auction đó

### Client cần update gì
- countdown
- end time
- thông báo auction được gia hạn

### Payload gợi ý
```json
{
  "eventType": "AuctionExtended",
  "auctionId": 1,
  "newEndTime": "2026-04-08T21:50:00",
  "reason": "Bid received near closing time"
}
```

---

## 6. Luồng event tiêu chuẩn khi có bid hợp lệ
### 6.1 Flow tổng quát
1. client gửi request bid
2. server nhận request
3. server lấy lock theo auction
4. server validate bid
5. server cập nhật state
6. server lưu bid
7. server tạo event
8. server unlock
9. server broadcast event tới client

### 6.2 Trình tự event đề xuất
Trong trường hợp bid hợp lệ:
- phát `BidPlaced`
- nếu leader đổi thì phát `LeaderChanged`
- nếu anti-sniping kích hoạt thì phát `AuctionExtended`

---

## 7. Luồng event khi auction kết thúc
1. countdown đạt điều kiện đóng phiên
2. server khóa auction hoặc chuyển trạng thái sang closed
3. xác định winner cuối
4. phát `AuctionFinished`
5. client dừng mọi thao tác bid

---

## 8. UI cần phản ứng thế nào
### 8.1 Khi nhận BidPlaced
UI cập nhật:
- bảng/lịch sử bid
- current price
- thông báo có bid mới

### 8.2 Khi nhận LeaderChanged
UI cập nhật:
- tên người dẫn đầu
- trạng thái nhấn mạnh leader mới
- current price mới

### 8.3 Khi nhận AuctionFinished
UI cập nhật:
- trạng thái closed
- khóa form đặt giá
- hiển thị winner
- hiển thị giá cuối cùng

### 8.4 Khi nhận AuctionExtended
UI cập nhật:
- countdown mới
- thông báo gia hạn

---

## 9. Cách nhóm dữ liệu theo auction
Server chỉ nên broadcast tới các client đang theo dõi đúng auction tương ứng.

Điều này có nghĩa:
- client xem auction 1 chỉ nhận event của auction 1
- client xem auction 2 không cần nhận event của auction 1

Nguyên tắc này giúp:
- giảm tải network
- dễ quản lý room/channel theo auction
- tránh client nhận thừa dữ liệu

---

## 10. Mối liên hệ với concurrency
Event flow chỉ đúng khi concurrency đúng.

Nếu lock strategy sai:
- event có thể phát sai thứ tự
- current price trong event có thể sai
- leader trong event có thể sai

Vì vậy:
- bid-lock-strategy và realtime-event-flow phải đi cùng nhau
- event chỉ phát sau khi state đã được chốt an toàn

---

## 11. Kết luận
Luồng realtime của hệ thống cần xoay quanh các event:
- `BidPlaced`
- `LeaderChanged`
- `AuctionFinished`

Có thể mở rộng thêm:
- `BidRejected`
- `AuctionExtended`

Nguyên tắc chung:
- state phải đúng trước
- event phát sau
- client chỉ nhận event của auction mà mình đang theo dõi
- UI update theo payload event, không tự suy đoán trạng thái
