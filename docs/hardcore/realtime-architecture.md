# Realtime Architecture - Auction Project

## 1. Mục tiêu tài liệu
Tài liệu này mô tả hướng kiến trúc realtime cho hệ thống đấu giá của nhóm, đặc biệt tập trung vào luồng đặt giá (bidding) và cập nhật trạng thái phiên đấu giá theo thời gian thực.

Mục tiêu chính:
- xác định phần nào dùng REST
- xác định phần nào dùng Socket
- giải thích lý do chọn kiến trúc này
- làm nền cho phase 2 và phase 3

---

## 2. Bài toán kỹ thuật
Trong hệ thống đấu giá, nhiều người dùng có thể:
- cùng xem một phiên đấu giá
- cùng đặt giá trong khoảng thời gian rất gần nhau
- cần thấy giá mới, người dẫn đầu mới và thời gian còn lại gần như ngay lập tức

Nếu chỉ dùng REST request-response thông thường, client phải liên tục gọi lại API để hỏi:
- có giá mới chưa
- ai đang dẫn đầu
- auction còn mở không

Cách này có các nhược điểm:
- cập nhật chậm
- tốn nhiều request
- tăng tải server
- dễ gây lệch trạng thái giữa các client
- không phù hợp với trải nghiệm live bidding

---

## 3. Quyết định kiến trúc
Nhóm chọn mô hình kết hợp:

- **REST API** cho các chức năng CRUD và truy vấn thông thường
- **Socket** cho các sự kiện realtime trong bidding

### 3.1 REST dùng cho
REST phù hợp với các chức năng:
- đăng ký tài khoản
- đăng nhập
- tạo sản phẩm
- cập nhật sản phẩm
- tạo phiên đấu giá
- lấy danh sách phiên đấu giá
- lấy chi tiết phiên đấu giá
- lấy lịch sử bid
- lấy thông tin user

### 3.2 Socket dùng cho
Socket phù hợp với các chức năng cần cập nhật ngay:
- có bid mới
- thay đổi current price
- thay đổi current leader
- auction sắp đóng hoặc đã đóng
- anti-sniping gia hạn thời gian
- client đang xem phiên cần được đồng bộ trạng thái

---

## 4. Lý do chọn REST + Socket
### 4.1 Vì sao không chỉ dùng REST
Nếu chỉ dùng REST:
- client phải polling liên tục
- dễ bị trễ 1 đến vài giây
- không hiệu quả khi nhiều người xem cùng một auction
- khó tạo cảm giác realtime trong demo

### 4.2 Vì sao không dùng Socket cho tất cả
Nếu dùng Socket cho mọi thứ:
- hệ thống phức tạp hơn mức cần thiết
- CRUD thông thường không cần realtime
- khó bảo trì hơn
- không tận dụng được lợi thế rõ ràng của REST trong các tác vụ standard

### 4.3 Kết luận
Kết hợp REST và Socket là hợp lý nhất:
- REST giữ cho hệ thống rõ ràng, dễ xây API
- Socket giải quyết phần live bidding là phần khó nhất của hệ thống

---

## 5. Kiến trúc tổng quát
### 5.1 Luồng CRUD thông thường
1. Client gửi request REST
2. Server xử lý business logic
3. Server lưu dữ liệu vào database
4. Server trả response cho client

Ví dụ:
- seller tạo item
- user xem danh sách auction
- user xem lịch sử bid

### 5.2 Luồng realtime bidding
1. Client gửi yêu cầu bid
2. Server nhận request bid
3. Server đưa request vào vùng xử lý thread-safe
4. Server validate bid
5. Server cập nhật current price, leader, bid history
6. Server lưu dữ liệu
7. Server broadcast event cho các client đang theo dõi auction đó
8. Client nhận event và update UI ngay

---

## 6. Thành phần chính
### 6.1 Client
Client có trách nhiệm:
- gọi REST để lấy dữ liệu ban đầu
- mở kết nối Socket khi vào màn hình live bidding
- nhận event realtime
- cập nhật giao diện:
  - current price
  - current leader
  - countdown
  - trạng thái auction

### 6.2 Server
Server có trách nhiệm:
- cung cấp REST API
- quản lý kết nối Socket
- xử lý bid theo cách thread-safe
- phát event realtime cho đúng nhóm client

### 6.3 Database
Database có trách nhiệm:
- lưu user
- lưu item
- lưu auction
- lưu bid history
- lưu trạng thái phiên đấu giá

---

## 7. Các nhóm sự kiện realtime chính
Hệ thống cần ít nhất các event sau:

- `BidPlaced`
- `LeaderChanged`
- `AuctionFinished`

Có thể mở rộng thêm:
- `BidRejected`
- `AuctionExtended`

---

## 8. Dữ liệu nào cần realtime
### 8.1 Cần realtime
- current price
- current leader
- auction status
- countdown
- bid history mới nhất

### 8.2 Không bắt buộc realtime
- danh sách tất cả sản phẩm
- thông tin hồ sơ người dùng
- quản lý item của seller
- báo cáo thống kê

---

## 9. Nguyên tắc thiết kế
### 9.1 Chỉ realtime ở nơi thật sự cần
Không đẩy toàn bộ hệ thống sang cơ chế realtime. Chỉ bidding và trạng thái phiên đang mở mới cần update ngay.

### 9.2 Một auction là một vùng đồng bộ riêng
Các bid trong cùng một auction cần được kiểm soát chặt. Các auction khác nhau phải có thể chạy song song.

### 9.3 Luồng realtime phải bám business rule
Mọi update realtime chỉ có ý nghĩa nếu current price, leader và trạng thái auction là đúng theo business rule.

### 9.4 Client phải lấy snapshot ban đầu trước
Khi user mở trang auction:
- trước hết client gọi REST để lấy dữ liệu ban đầu
- sau đó mới dùng Socket để nhận thay đổi tiếp theo

Điều này giúp tránh trạng thái rỗng khi vừa vào màn hình.

---

## 10. Hướng triển khai theo phase
### Phase 1
- chốt kiến trúc REST + Socket
- mô tả luồng event
- làm prototype thread-safe bidding

### Phase 2
- dựng nền lock per auction
- chuẩn bị module concurrency

### Phase 3
- triển khai socket realtime
- broadcast event thật
- xử lý concurrent bidding thật

### Phase 4
- tối ưu reconnect
- tối ưu auto-bid, anti-sniping

### Phase 5
- stress test
- fix bug realtime
- viết tài liệu giải thích cơ chế

---

## 11. Kết luận
Kiến trúc phù hợp cho hệ thống đấu giá là:
- REST cho CRUD và truy vấn
- Socket cho bidding realtime

Cách tiếp cận này giúp hệ thống:
- rõ ràng
- dễ phát triển
- đủ mạnh cho demo live bidding
- phù hợp với vai trò HARDCORE trong nhóm
