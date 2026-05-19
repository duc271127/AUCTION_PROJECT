# Chốt với UI — HARDCORE Phase 1

## Mục tiêu
Tài liệu này dùng để chốt giữa HARDCORE và UI trong Phase 1, nhằm thống nhất phần realtime bidding, các trạng thái màn hình, và dữ liệu UI cần nhận từ backend/server.

---

## 1. Mục đích làm việc với UI
HARDCORE không làm UI chính, nhưng phải phối hợp với UI để biết:

- màn hình nào cần realtime
- field nào cần update ngay
- event nào là bắt buộc
- khi có lỗi hoặc bid bị reject thì UI hiển thị ra sao

Nếu không chốt sớm với UI, event flow sẽ bị thừa hoặc thiếu dữ liệu.

---

## 2. Những màn hình UI liên quan trực tiếp tới HARDCORE
Trong Phase 1, HARDCORE cần chốt với UI ít nhất các màn hình sau:

### 2.1 Auction List
Màn hình danh sách phiên đấu giá.

UI cần biết:
- có hiển thị current price ngay ở list không
- có hiển thị trạng thái OPEN / CLOSED không
- có cần refresh realtime ở list không hay chỉ refresh khi vào detail

### 2.2 Auction Detail / Live Bidding
Đây là màn hình quan trọng nhất.

UI cần chốt rõ:
- current price
- current leader
- thời gian còn lại
- bid history gần nhất
- ô nhập giá bid
- thông báo bid thành công / thất bại
- trạng thái auction đã đóng

### 2.3 Notification / Toast / Error Dialog
UI cần quyết định:
- bid mới hiển thị kiểu gì
- lỗi bid bị reject hiện ở đâu
- auction finished hiện kiểu gì
- anti-sniping extension có popup/toast hay không

---

## 3. Những câu hỏi HARDCORE phải chốt với UI

## 3.1 UI cần realtime những gì?
HARDCORE phải hỏi UI:

- Có cần update realtime `currentPrice` không?
- Có cần update realtime `currentLeader` không?
- Có cần update realtime `countdown` không?
- Có cần update realtime `bidHistory` không?
- Có cần update realtime trạng thái `OPEN/CLOSED` không?

### Kết luận nên chốt
Ở màn hình live bidding, UI nên nhận realtime ít nhất:
- `currentPrice`
- `currentLeader`
- `remainingTime`
- `auctionStatus`

---

## 3.2 UI cần hiển thị gì khi có bid mới?
HARDCORE phải hỏi UI:

- Khi có bid mới, chỉ đổi số giá hay còn hiện toast?
- Có highlight leader mới không?
- Có cập nhật danh sách bid mới nhất không?

### Kết luận nên chốt
Khi nhận event `BidPlaced`, UI nên:
- cập nhật `currentPrice`
- thêm 1 dòng vào `bidHistory`
- có thể hiện toast ngắn “Có bid mới”

---

## 3.3 UI cần hiển thị gì khi leader đổi?
HARDCORE phải hỏi UI:

- Có cần đổi nhãn “đang dẫn đầu” không?
- Có cần highlight màu không?
- Có cần hiện tên người đang dẫn đầu không?

### Kết luận nên chốt
Khi nhận event `LeaderChanged`, UI nên:
- cập nhật `currentLeader`
- highlight leader mới
- đồng bộ với current price mới nhất

---

## 3.4 UI cần hiển thị gì khi auction đóng?
HARDCORE phải hỏi UI:

- Khi auction finished thì khóa những nút nào?
- Có hiện winner không?
- Có dừng countdown không?
- Có hiện popup “Auction finished” không?

### Kết luận nên chốt
Khi nhận event `AuctionFinished`, UI nên:
- khóa form bid
- dừng countdown
- hiển thị winner
- đổi trạng thái sang CLOSED

---

## 3.5 UI muốn xử lý lỗi bid như thế nào?
HARDCORE phải hỏi UI:

- Bid thấp hơn current price thì hiện lỗi ở đâu?
- Auction đã đóng thì hiện lỗi ở đâu?
- Dùng dialog, label đỏ hay toast?

### Kết luận nên chốt
Với lỗi bid:
- ưu tiên hiện message gần form bid
- có thể kèm dialog/toast ngắn
- không đổi current price và leader trên UI

---

## 4. Event realtime cần thống nhất với UI

## 4.1 BidPlaced
### Ý nghĩa
Một bid hợp lệ đã được chấp nhận.

### UI cần update
- `currentPrice`
- `bidHistory`

### Payload tối thiểu gợi ý
```json
{
  "eventType": "BidPlaced",
  "auctionId": 1,
  "bidderName": "UserA",
  "bidAmount": 120.0,
  "currentPrice": 120.0,
  "timestamp": "2026-04-08T21:30:00"
}
```

---

## 4.2 LeaderChanged
### Ý nghĩa
Người dẫn đầu đã thay đổi.

### UI cần update
- `currentLeader`
- highlight leader

### Payload tối thiểu gợi ý
```json
{
  "eventType": "LeaderChanged",
  "auctionId": 1,
  "leaderName": "UserA",
  "currentPrice": 120.0,
  "timestamp": "2026-04-08T21:30:00"
}
```

---

## 4.3 AuctionFinished
### Ý nghĩa
Phiên đấu giá kết thúc.

### UI cần update
- trạng thái CLOSED
- winner
- khóa form bid
- dừng countdown

### Payload tối thiểu gợi ý
```json
{
  "eventType": "AuctionFinished",
  "auctionId": 1,
  "winnerName": "UserA",
  "finalPrice": 150.0,
  "timestamp": "2026-04-08T21:45:00"
}
```

---

## 4.4 BidRejected
### Ý nghĩa
Bid không hợp lệ.

### UI cần update
- hiển thị lỗi
- giữ nguyên state hiện tại

### Payload tối thiểu gợi ý
```json
{
  "eventType": "BidRejected",
  "auctionId": 1,
  "message": "Bid amount must be greater than current price"
}
```

---

## 5. Dữ liệu UI nên lấy qua REST, dữ liệu nào nên nhận qua realtime

## 5.1 Lấy qua REST
- danh sách auction ban đầu
- chi tiết auction ban đầu
- lịch sử bid ban đầu
- thông tin sản phẩm
- thông tin seller

## 5.2 Nhận qua realtime
- giá mới nhất
- leader mới nhất
- countdown / time extension
- trạng thái auction khi đóng
- bid history mới phát sinh sau khi client đã vào màn hình

---

## 6. Luồng phối hợp giữa HARDCORE và UI
### Khi user mở trang auction detail
1. UI gọi REST lấy snapshot ban đầu
2. UI render đầy đủ dữ liệu đầu tiên
3. UI mở kết nối realtime theo auctionId
4. HARDCORE đảm bảo event mới đẩy xuống đúng auction đó
5. UI cập nhật màn hình khi có event mới

### Khi user đặt bid
1. UI gửi request bid
2. HARDCORE/Server xử lý thread-safe
3. Nếu bid hợp lệ:
   - server cập nhật state
   - phát event realtime
   - UI update giá, leader, history
4. Nếu bid không hợp lệ:
   - UI hiển thị lỗi
   - state màn hình giữ nguyên

---

## 7. Checklist cần chốt với UI trong Phase 1
- [ ] Màn hình nào cần realtime
- [ ] Field nào cần realtime
- [ ] Có cần realtime ở auction list không
- [ ] Khi có bid mới UI update gì
- [ ] Khi leader đổi UI update gì
- [ ] Khi auction finished UI update gì
- [ ] Lỗi bid hiện kiểu gì
- [ ] Toast/dialog/label đỏ dùng ở đâu
- [ ] Countdown có cần đồng bộ realtime không
- [ ] Tên event dùng chung cho cả nhóm là gì

---

## 8. Kết luận
Trong Phase 1, HARDCORE cần chốt với UI để đảm bảo:
- event realtime không thừa
- payload không thiếu dữ liệu
- live bidding screen có thể update đúng
- UI và phần concurrency/realtime của HARDCORE bám đúng nhau ngay từ đầu

Đây là tài liệu nền để sang phase 2 và phase 3 triển khai realtime thật.
