# Bidding Lock Strategy - Auction Project

## 1. Mục tiêu tài liệu
Tài liệu này mô tả chiến lược khóa (lock strategy) cho quá trình đặt giá trong hệ thống đấu giá.

Mục tiêu:
- ngăn race condition
- tránh lost update
- đảm bảo current price và current leader luôn đúng
- làm nền cho concurrent bidding ở phase sau

---

## 2. Bài toán đặt ra
Trong hệ thống đấu giá, nhiều người dùng có thể bid vào cùng một auction gần như đồng thời.

Ví dụ:
- current price hiện tại là 100
- user A bid 120
- user B bid 130
- hai request đến gần như cùng lúc

Nếu không có cơ chế đồng bộ:
- nhiều thread cùng đọc current price cũ
- nhiều thread cùng ghi đè state
- leader có thể sai
- current price có thể sai
- bid history có thể không nhất quán

Đây là vấn đề điển hình của concurrency.

---

## 3. Mục tiêu của lock strategy
Lock strategy cần đảm bảo:
- tại một thời điểm chỉ có một luồng xử lý bid cho cùng một auction
- bid của auction A không chặn bid của auction B
- state cập nhật theo thứ tự an toàn
- không có 2 người cùng thắng
- không có current price bị rollback

---

## 4. Quyết định thiết kế
### 4.1 Dùng lock per auction
Mỗi auction có một lock riêng.

Điều này có nghĩa:
- các bid vào cùng một auction sẽ được xử lý tuần tự trong vùng critical section
- các bid vào auction khác nhau vẫn có thể xử lý song song

Đây là cách cân bằng giữa:
- tính đúng đắn
- khả năng mở rộng

### 4.2 Không dùng một lock global cho toàn hệ thống
Không nên khóa toàn bộ hệ thống vì:
- làm giảm hiệu năng
- tất cả auction sẽ bị nối đuôi nhau không cần thiết
- không tận dụng được tính song song của nhiều auction độc lập

---

## 5. Shared state cần bảo vệ
Với mỗi auction, các dữ liệu sau được xem là shared state:
- current price
- current leader
- auction status
- bid history
- end time trong trường hợp anti-sniping

Các phần này không được để nhiều thread cập nhật tự do.

---

## 6. Critical section trong bid flow
Critical section là đoạn logic cần được bảo vệ bởi lock.

### 6.1 Các bước nên nằm trong critical section
1. lấy trạng thái auction hiện tại
2. kiểm tra auction còn mở không
3. kiểm tra bid có hợp lệ không
4. cập nhật current price
5. cập nhật current leader
6. thêm bid vào lịch sử
7. xử lý anti-sniping nếu có
8. tạo event cần broadcast

### 6.2 Không nên để ngoài critical section
Các công việc có thể cân nhắc để ngoài vùng khóa:
- logging không quan trọng
- chuẩn hóa response
- broadcast thực tế tới client sau khi state đã chốt
- các thao tác không đụng shared state

Mục tiêu là giữ critical section ngắn nhưng vẫn đủ an toàn.

---

## 7. Luồng xử lý đề xuất
### 7.1 Pseudo flow
1. nhận request bid
2. xác định auctionId
3. lấy lock tương ứng với auctionId
4. lock
5. validate business rule
6. cập nhật state
7. lưu dữ liệu
8. tạo event
9. unlock
10. broadcast event

### 7.2 Pseudo code
```java
lock(auctionId)
try {
    validateAuctionOpen()
    validateBidAmount()
    updateCurrentPrice()
    updateLeader()
    saveBidHistory()
    buildRealtimeEvent()
} finally {
    unlock(auctionId)
}
broadcastEvent()
```

---

## 8. Xử lý khi hai bid đến gần nhau
### 8.1 Trường hợp cơ bản
Giả sử:
- current price = 100
- A bid 120
- B bid 130

Nếu dùng lock per auction:
- một thread vào trước xử lý trước
- thread còn lại phải chờ
- sau khi thread đầu xong, thread sau xử lý trên state mới nhất

Kết quả:
- state cuối nhất quán
- leader cuối đúng
- current price cuối đúng

### 8.2 Trường hợp bid cùng mức giá
Nếu hai user cùng bid một mức bằng nhau, cần thống nhất rule với CORE LOGIC:
- ai đến trước thì ưu tiên
hoặc
- hệ thống không cho trùng mức trong một số trường hợp

Tạm thời, HARDCORE đề xuất:
- ưu tiên theo thứ tự bid được server chấp nhận trong vùng lock
- timestamp server là nguồn tham chiếu chính

---

## 9. Nguồn thời gian và thứ tự ưu tiên
### 9.1 Không nên tin hoàn toàn vào timestamp từ client
Lý do:
- client có thể lệch giờ
- client có thể gửi chậm
- mạng không ổn định

### 9.2 Nên ưu tiên thứ tự xử lý tại server
Đề xuất:
- thứ tự chấp nhận bid được xác định theo thời điểm server nhận và xử lý trong lock
- timestamp server là chuẩn chính

Điều này dễ kiểm soát hơn và giảm tranh cãi khi demo.

---

## 10. Những lỗi lock strategy cần tránh
### 10.1 Lock quá rộng
Nếu khóa quá nhiều phần:
- giảm hiệu năng
- làm hệ thống chậm

### 10.2 Lock quá hẹp
Nếu để một phần cập nhật state ở ngoài vùng lock:
- current price và leader có thể lệch nhau
- bid history có thể không đồng bộ

### 10.3 Quên unlock
Nếu dùng `ReentrantLock` mà không unlock trong `finally`:
- dễ gây treo luồng
- lock bị giữ mãi

### 10.4 Khóa toàn hệ thống
Không nên dùng một lock cho mọi auction vì không cần thiết.

---

## 11. Gợi ý hiện thực
### 11.1 Phase 1
Dùng `synchronized` hoặc `ReentrantLock` trong prototype để chứng minh ý tưởng.

### 11.2 Phase 2 trở đi
Nên tách riêng thành các thành phần:
- `AuctionLockManager`
- `ConcurrentBidProcessor`

`AuctionLockManager` giữ map:
- key: auctionId
- value: lock tương ứng

---

## 12. Tương tác với các module khác
### 12.1 Với CORE LOGIC
Cần chốt:
- bid hợp lệ là gì
- auction đóng khi nào
- trường hợp bid đồng mức xử lý ra sao

### 12.2 Với SERVER + DATA
Cần chốt:
- lưu bid ở bước nào
- transaction database xử lý ra sao
- payload event gồm những field nào

### 12.3 Với UI
Cần chốt:
- UI nhận event gì
- khi reject bid thì UI hiển thị thế nào

---

## 13. Kết luận
Chiến lược khóa phù hợp cho hệ thống là:
- mỗi auction có một lock riêng
- toàn bộ cập nhật shared state phải đi qua critical section
- thứ tự xử lý bid được quyết định tại server
- không dùng global lock cho toàn bộ hệ thống

Đây là nền tảng để triển khai:
- concurrent bidding
- auto-bid
- anti-sniping
- realtime broadcasting
ở các phase tiếp theo.
