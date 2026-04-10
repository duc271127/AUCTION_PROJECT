# Business Rules - Auction Project

## 1. Mục tiêu tài liệu
Tài liệu này xác định các luật nghiệp vụ cốt lõi của hệ thống đấu giá, nhằm thống nhất giữa:

- CORE LOGIC
- HARDCORE
- SERVER + DATA
- UI

Tài liệu này tập trung vào các nhóm rule chính:

- người dùng
- sản phẩm
- phiên đấu giá
- đặt giá
- kết thúc phiên
- lỗi và ngoại lệ
- realtime và concurrency

---

## 2. Phạm vi hệ thống

Hệ thống cho phép:

- người bán tạo sản phẩm và mở phiên đấu giá
- người dùng tham gia đấu giá
- hệ thống xác định người dẫn đầu và người thắng
- hệ thống cập nhật trạng thái phiên theo thời gian thực
- hệ thống xử lý đúng khi nhiều người bid gần như cùng lúc

---

## 3. Vai trò người dùng

## 3.1 Guest
Người chưa đăng nhập.

### Guest được phép:
- xem danh sách auction
- xem chi tiết auction

### Guest không được phép:
- tạo sản phẩm
- tạo phiên đấu giá
- đặt bid
- xem thông tin quản lý cá nhân

---

## 3.2 Bidder
Người dùng đã đăng nhập và có quyền tham gia đấu giá.

### Bidder được phép:
- xem auction
- đặt bid
- xem lịch sử bid của phiên đấu giá
- xem trạng thái mình đang dẫn đầu hay không

### Bidder không được phép:
- đặt bid vào auction do chính mình tạo
- sửa dữ liệu auction của người khác

---

## 3.3 Seller
Người bán.

### Seller được phép:
- tạo sản phẩm
- tạo auction cho sản phẩm của mình
- sửa sản phẩm khi auction chưa bắt đầu
- xem danh sách auction của mình

### Seller không được phép:
- bid vào auction của chính mình
- sửa auction khi auction đã mở nếu rule nhóm chốt là không cho phép

---

## 3.4 Admin
Quản trị viên.

### Admin được phép:
- xem và quản lý user
- xem và quản lý item
- xem và quản lý auction
- hủy auction nếu có lý do hợp lệ
- khóa user nếu vi phạm

---

# 4. Rule về User

## 4.1 Đăng ký tài khoản
Một user hợp lệ khi có:
- họ tên hợp lệ
- email hợp lệ
- password hợp lệ
- role hợp lệ

## 4.2 Email
- email phải là duy nhất
- không được trùng với user khác
- email nên được chuẩn hóa trước khi lưu

## 4.3 Password
- không lưu plain text
- phải được mã hóa trước khi lưu
- không được để trống

## 4.4 Trạng thái user
Gợi ý:
- ACTIVE
- INACTIVE
- BLOCKED

### Rule:
- user ở trạng thái BLOCKED không được bid
- user INACTIVE không được đăng nhập hoặc không được tham gia tùy rule nhóm chốt

---

# 5. Rule về sản phẩm (Item)

## 5.1 Một item thuộc về một seller
- mỗi sản phẩm phải có đúng một owner
- owner của item là seller tạo ra item đó

## 5.2 Item hợp lệ để tạo auction khi:
- có tên
- có mô tả
- có giá khởi điểm hoặc thông tin để tạo giá khởi điểm
- không ở trạng thái bị xóa hoặc khóa

## 5.3 Quan hệ item và auction
Có thể chốt theo 1 trong 2 hướng:

### Hướng A
Một item chỉ thuộc một auction tại một thời điểm.

### Hướng B
Một item có thể được đấu giá lại sau khi auction cũ kết thúc.

### Rule nên dùng
- một item không được nằm trong hai auction OPEN cùng lúc

---

# 6. Rule về Auction

## 6.1 Trạng thái auction
Nên thống nhất các trạng thái sau:

- DRAFT
- SCHEDULED
- OPEN
- CLOSED
- CANCELLED

Nếu làm anti-sniping nâng cao có thể thêm:
- EXTENDED

---

## 6.2 Ý nghĩa trạng thái

### DRAFT
- auction mới tạo
- chưa mở cho người dùng bid

### SCHEDULED
- auction đã được cấu hình thời gian
- chưa đến thời điểm mở

### OPEN
- đang cho phép bid

### CLOSED
- đã kết thúc
- không nhận bid mới

### CANCELLED
- bị hủy
- không nhận bid mới
- không có winner

---

## 6.3 Điều kiện để một auction hợp lệ
Một auction phải có:
- item hợp lệ
- seller hợp lệ
- start time
- end time
- start price
- trạng thái ban đầu phù hợp

## 6.4 Rule thời gian
- `startTime < endTime`
- auction chỉ được mở khi current time >= startTime
- auction chỉ được coi là đóng khi current time > endTime hoặc có rule đóng cụ thể

## 6.5 Rule giá khởi điểm
- start price phải lớn hơn 0
- current price ban đầu = start price
- khi chưa có bid nào, current leader = null

---

# 7. Rule về Bid

Đây là phần quan trọng nhất.

## 7.1 Điều kiện để bid hợp lệ
Một bid hợp lệ khi đồng thời thỏa tất cả điều kiện sau:

- user đã đăng nhập
- user ở trạng thái được phép bid
- auction đang ở trạng thái OPEN
- current time nằm trong thời gian cho phép bid
- user không phải là seller của auction đó
- bid amount lớn hơn current price theo rule hệ thống
- request bid không vi phạm rule đồng thời

---

## 7.2 Rule mức giá bid
Có 2 cách chốt:

### Cách 1
`bidAmount > currentPrice`

### Cách 2
`bidAmount >= currentPrice + minIncrement`

### Khuyến nghị
Dùng **bước giá tối thiểu** để hệ thống rõ ràng hơn.

Ví dụ:
- `minIncrement = 1`
hoặc
- `minIncrement = 10.000`

### Rule đề xuất
- bid hợp lệ khi `bidAmount >= currentPrice + minIncrement`

---

## 7.3 Không được bid vào auction của chính mình
- seller không được đặt bid vào auction do mình tạo
- nếu vi phạm thì reject request

---

## 7.4 Mỗi bid hợp lệ phải tạo ra các thay đổi sau
Khi một bid được chấp nhận, hệ thống phải cập nhật **nguyên tử**:

- current price
- current leader
- bid history
- trạng thái liên quan nếu có anti-sniping
- event realtime tương ứng

---

## 7.5 Bid không hợp lệ
Bid bị reject trong các trường hợp:
- thấp hơn current price
- không đạt min increment
- auction đã CLOSED
- auction chưa OPEN
- user không hợp lệ
- user là seller của auction
- request tới quá muộn

---

# 8. Rule về current price, current leader, bid history

## 8.1 Current price
- current price luôn bằng giá của bid hợp lệ cao nhất hiện tại
- nếu chưa có bid nào, current price = start price

## 8.2 Current leader
- current leader là user của bid hợp lệ cao nhất hiện tại
- nếu chưa có bid nào, current leader = null

## 8.3 Bid history
- mọi bid hợp lệ phải được lưu vào lịch sử bid
- bid history phải có:
  - bidder
  - bid amount
  - timestamp server
  - auctionId

## 8.4 Server timestamp là nguồn chuẩn
- không dùng timestamp từ client làm nguồn quyết định chính
- thứ tự bid phải dựa trên thứ tự server nhận/xử lý

---

# 9. Rule xử lý đồng thời (Concurrency Rule)

Đây là phần HARDCORE cần bám sát.

## 9.1 Shared state
Với mỗi auction, các dữ liệu sau là shared state:
- current price
- current leader
- bid history
- end time nếu có anti-sniping
- auction status

## 9.2 Lock strategy
- mỗi auction phải có một lock riêng
- các bid vào cùng một auction phải được xử lý tuần tự trong vùng critical section
- các auction khác nhau có thể xử lý song song

## 9.3 Không dùng global lock
- không nên dùng một lock cho toàn bộ hệ thống
- nếu dùng global lock, mọi auction sẽ bị chặn lẫn nhau

## 9.4 Rule xử lý 2 bid cùng lúc
Nếu hai bid vào cùng một auction gần như đồng thời:
- chỉ một bid được vào critical section trước
- bid vào sau phải xử lý trên state mới nhất
- thứ tự quyết định theo thời điểm server chấp nhận xử lý trong lock

## 9.5 Tie-breaking rule
Nếu hai bid bằng nhau và cùng hợp lệ theo mặt số học:
- bid được server chấp nhận trước sẽ được ưu tiên
- bid sau bị reject hoặc không trở thành leader tùy rule nhóm chốt

### Khuyến nghị
- không cho phép bid bằng current price
- nếu bằng nhau thì không thắng leader hiện tại

---

# 10. Rule về realtime

## 10.1 Event bắt buộc
Tối thiểu cần có các event:

- `BidPlaced`
- `LeaderChanged`
- `AuctionFinished`

Có thể thêm:
- `BidRejected`
- `AuctionExtended`

## 10.2 Khi nào phát event
Chỉ phát event sau khi:
- bid được validate xong
- shared state được cập nhật xong
- logic nghiệp vụ đã chốt

## 10.3 UI cần update gì
Khi có bid hợp lệ:
- current price
- current leader
- bid history
- countdown nếu cần

Khi auction finished:
- trạng thái CLOSED
- winner
- final price
- khóa form bid

---

# 11. Rule về kết thúc auction

## 11.1 Điều kiện đóng auction
Auction kết thúc khi:
- current time vượt quá end time
hoặc
- admin hủy auction
hoặc
- có rule đặc biệt khác do nhóm định nghĩa

## 11.2 Khi auction CLOSED
- không nhận bid mới
- current leader cuối cùng trở thành winner
- final price = current price cuối cùng

## 11.3 Nếu auction không có bid nào
- winner = null
- final price = start price hoặc null tùy cách nhóm hiển thị
- auction vẫn chuyển sang CLOSED

## 11.4 Nếu auction bị CANCELLED
- không có winner
- không nhận bid mới
- cần hiển thị rõ lý do hủy nếu có

---

# 12. Rule về winner

## 12.1 Winner là ai
Winner là:
- user đang là current leader tại thời điểm auction đóng

## 12.2 Điều kiện để có winner
- auction phải ở trạng thái CLOSED
- phải có ít nhất một bid hợp lệ

## 12.3 Không có winner khi:
- auction không có bid hợp lệ nào
- auction bị CANCELLED

---

# 13. Rule về Auto-bid (nâng cao)

Phần này có thể là phase sau, nhưng nên chốt nền từ sớm.

## 13.1 Auto-bid là gì
User có thể đặt một mức trần tối đa. Hệ thống tự động tăng giá thay user khi có bid cạnh tranh.

## 13.2 Rule cơ bản
- max bid phải lớn hơn current price
- hệ thống chỉ tăng tới mức cần thiết để giữ leader
- không vượt quá max bid user đã đặt

## 13.3 Khi nhiều auto-bid cùng tồn tại
- phải có rule ưu tiên rõ ràng
- thường ưu tiên theo thứ tự server chấp nhận max bid trước

---

# 14. Rule về Anti-sniping (nâng cao)

## 14.1 Anti-sniping là gì
Nếu có bid hợp lệ quá gần thời điểm đóng phiên, hệ thống sẽ gia hạn thêm thời gian.

## 14.2 Rule gợi ý
- nếu bid hợp lệ đến trong X giây cuối
- thì end time được cộng thêm Y giây/phút

Ví dụ:
- bid đến trong 30 giây cuối
- hệ thống gia hạn thêm 2 phút

## 14.3 Điều cần chốt
- gia hạn mấy lần
- mỗi lần gia hạn bao lâu
- có giới hạn tổng số lần gia hạn không

---

# 15. Rule về lỗi và ngoại lệ

## 15.1 Các lỗi nghiệp vụ chính
Hệ thống nên có các lỗi như:

- `USER_NOT_FOUND`
- `USER_BLOCKED`
- `ITEM_NOT_FOUND`
- `AUCTION_NOT_FOUND`
- `AUCTION_NOT_OPEN`
- `AUCTION_ALREADY_CLOSED`
- `BID_AMOUNT_TOO_LOW`
- `SELLER_CANNOT_BID_OWN_AUCTION`
- `UNAUTHORIZED_ACTION`

## 15.2 Rule hiển thị lỗi
- lỗi phải rõ ràng
- lỗi bid không hợp lệ phải trả về message dễ hiểu cho UI
- lỗi không được làm thay đổi current price/current leader

---

# 16. Rule về tính nhất quán dữ liệu

## 16.1 Atomic update
Khi bid hợp lệ, các thay đổi sau phải đi cùng nhau:
- current price
- current leader
- bid history

Không được có trạng thái:
- current price đã đổi nhưng leader chưa đổi
- leader đã đổi nhưng bid history chưa lưu

## 16.2 Server là nguồn sự thật
- UI chỉ hiển thị theo dữ liệu server trả về
- client không tự quyết định ai đang thắng

---

# 17. Rule về audit / logging

## 17.1 Các hành động nên log
- tạo auction
- đặt bid
- reject bid
- đóng auction
- hủy auction

## 17.2 Log bid nên có
- auctionId
- bidderId
- bidAmount
- timestamp server
- accepted/rejected
- reason nếu rejected

---

# 18. Kịch bản mẫu

## 18.1 Kịch bản bid hợp lệ
- current price = 100
- min increment = 10
- user A bid 120
- bid hợp lệ
- current price = 120
- current leader = user A

## 18.2 Kịch bản bid không hợp lệ
- current price = 120
- user B bid 125
- nếu min increment = 10 thì bid không hợp lệ
- hệ thống reject
- current price giữ nguyên 120
- leader giữ nguyên user A

## 18.3 Kịch bản 2 người bid cùng lúc
- current price = 100
- user A bid 120
- user B bid 130
- cả hai request đến gần nhau
- hệ thống lock theo auction
- một request xử lý trước, request còn lại xử lý sau
- final state phải nhất quán:
  - current price = 130
  - current leader = user B

---

# 19. Kết luận
Business rule cốt lõi của hệ thống là:

- chỉ user hợp lệ mới được bid
- chỉ auction OPEN mới nhận bid
- bid phải vượt current price theo rule bước giá
- seller không được bid auction của mình
- current price, current leader, bid history phải cập nhật nguyên tử
- winner là current leader tại thời điểm auction đóng
- xử lý đồng thời phải theo lock per auction
- realtime chỉ phát sau khi state đã được chốt đúng
