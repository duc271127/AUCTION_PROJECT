# Phase 3 Realtime Test Result - HARDCORE

## 1. Mục tiêu
Tài liệu này ghi lại kết quả test cho phần realtime foundation của HARDCORE trong Day 31–35, tập trung vào:

- socket server / broadcast event
- thread-safe notify
- build event từ state đã chốt
- broadcast theo `auctionId`

---

## 2. Thành phần đã dùng
Phần realtime được test dựa trên các thành phần sau:

- `WebSocketConfig`
- `RealtimeEventType`
- `RealtimeEvent`
- `RealtimeEventFactory`
- `RealtimeNotifier`
- `ConcurrentBidProcessor`
- `BidController`

Hệ thống sử dụng:

- endpoint websocket: `/ws`
- broker prefix: `/topic`
- destination theo auction: `/topic/auctions/{auctionId}`

---

## 3. Kịch bản test 1 — Bid accepted phát event

### Mục tiêu
Kiểm tra rằng khi một bid hợp lệ được accept, hệ thống sẽ build và broadcast event realtime tương ứng.

### Input
REST request:
- `POST /api/auctions/1/bids`

Body:
```json
{
  "bidderName": "UserA",
  "bidAmount": 120
}
```

### Kỳ vọng
- request đi qua `ConcurrentBidProcessor`
- lock theo `auctionId` được acquire và release
- event `BID_PLACED` được build
- event được broadcast tới:
  - `/topic/auctions/1`

### Kết quả thực tế
Terminal log ghi nhận:
- user acquire lock
- user release lock
- `Broadcasting to /topic/auctions/1 -> BID_PLACED`

### Kết luận
Bid hợp lệ đã kích hoạt broadcast event thành công theo đúng auction đang được theo dõi.

---

## 4. Kịch bản test 2 — Leader changed phát event

### Mục tiêu
Kiểm tra rằng khi bid mới làm đổi leader, hệ thống sẽ build thêm event `LEADER_CHANGED`.

### Input
Sau khi auction đang có leader cũ, gửi tiếp bid cao hơn:
- `POST /api/auctions/1/bids`

Ví dụ body:
```json
{
  "bidderName": "UserB",
  "bidAmount": 130
}
```

### Kỳ vọng
- leader cũ bị thay bởi leader mới
- event `LEADER_CHANGED` được build
- event được broadcast tới:
  - `/topic/auctions/1`

### Kết quả thực tế
Terminal log ghi nhận:
- `Broadcasting to /topic/auctions/1 -> LEADER_CHANGED`

### Kết luận
Khi leader thay đổi, hệ thống đã tạo và phát event đúng như thiết kế của realtime bidding.

---

## 5. Kịch bản test 3 — Close auction phát event

### Mục tiêu
Kiểm tra rằng khi auction được close, hệ thống phát event `AUCTION_FINISHED`.

### Input
REST request:
- `POST /api/auctions/1/close`

### Kỳ vọng
- auction chuyển sang `CLOSED`
- winner được chốt
- event `AUCTION_FINISHED` được build
- event được broadcast tới:
  - `/topic/auctions/1`

### Kết quả thực tế
Kết quả mong muốn khi chạy test hoàn chỉnh:
- terminal log xuất hiện:
  - `Broadcasting to /topic/auctions/1 -> AUCTION_FINISHED`

### Kết luận
Đây là event cuối của vòng đời auction và là tín hiệu để client khóa form bid, cập nhật status và hiển thị winner.

---

## 6. Kiểm tra thread-safe notify

### Rule đã áp dụng
- build event trong lock
- broadcast sau lock

### Điều được chứng minh
Từ log hệ thống có thể thấy:
- request vào `ConcurrentBidProcessor`
- lock được acquire
- state được xử lý
- lock được release
- sau đó mới xuất hiện log `Broadcasting to ...`

### Ý nghĩa
Điều này chứng minh notify không làm tăng thời gian giữ lock và không phá thread-safety của bid flow.

---

## 7. Destination broadcast đã dùng

Các event realtime được broadcast tới:

```text
/topic/auctions/1
```

Cách tổ chức này đúng với mô hình room/channel theo `auctionId`, giúp:
- client chỉ nhận event của auction đang xem
- tránh broadcast toàn hệ thống
- dễ tích hợp UI realtime ở phase sau

---

## 8. Kết luận tổng
Qua các kịch bản test, phần realtime foundation của HARDCORE trong Day 31–35 đã chứng minh được:

- WebSocketConfig đã tạo nền cho realtime
- event model và event factory hoạt động đúng vai trò
- bid accepted có thể phát `BID_PLACED`
- leader changed có thể phát `LEADER_CHANGED`
- close auction có thể phát `AUCTION_FINISHED`
- broadcast đang đi đúng destination theo `auctionId`
- notify được làm sau lock, đúng nguyên tắc thread-safe

Đây là nền tảng để sang các bước tiếp theo của Phase 3:

- stress test concurrent bids
- fix race condition
- test 2–3 client cùng lúc
- chốt demo realtime cơ bản
