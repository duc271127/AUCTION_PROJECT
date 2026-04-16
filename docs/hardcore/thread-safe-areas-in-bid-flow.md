# Thread-Safe Areas in Bid Flow

## 1. Mục tiêu tài liệu
Tài liệu này xác định rõ phần nào trong bid flow phải được xử lý thread-safe, phần nào có thể để ngoài vùng khóa, và lý do kỹ thuật phía sau.

Mục tiêu:
- tránh race condition
- tránh lost update
- đảm bảo current price và current leader luôn đúng
- làm nền cho concurrent bidding, auto-bid và anti-sniping ở các phase sau

---

## 2. Shared state của một auction
Trong hệ thống đấu giá, với mỗi auction, các dữ liệu sau được xem là shared state và có nguy cơ bị nhiều thread truy cập cùng lúc:

- `currentPrice`
- `currentLeader`
- `auctionStatus`
- `endTime` nếu có anti-sniping
- `bidHistory`
- `winner` khi chốt phiên

Nếu nhiều thread cùng đọc/ghi các dữ liệu này mà không có cơ chế bảo vệ, hệ thống có thể sai trạng thái cuối.

---

## 3. Nguyên tắc xác định chỗ cần thread-safe
Một đoạn trong bid flow phải thread-safe nếu rơi vào ít nhất một trong các trường hợp sau:

- đọc state hiện tại của auction để ra quyết định nghiệp vụ
- ghi vào state dùng chung của auction
- có thể làm lệch giữa `currentPrice`, `currentLeader`, `bidHistory`, `auctionStatus`
- nếu hai request cùng chạy ở đoạn đó thì có thể làm sai kết quả cuối

---

## 4. Các chỗ bắt buộc phải thread-safe trong bid flow

## 4.1 Đọc trạng thái auction hiện tại
Bao gồm:
- đọc `currentPrice`
- đọc `auctionStatus`
- đọc `endTime`
- đọc `currentLeader`

### Vì sao phải thread-safe
Các giá trị này là nền để validate và xử lý bid. Nếu hai thread cùng đọc state cũ rồi cùng xử lý tiếp, logic phía sau sẽ sai.

### Ví dụ
- current price = 100
- thread A đọc 100
- thread B cũng đọc 100
- A định nâng lên 120
- B định nâng lên 130

Nếu không được bảo vệ, hệ thống có thể tính toán dựa trên state cũ thay vì state mới nhất.

---

## 4.2 Validate bid theo state hiện tại
Bao gồm các kiểm tra:
- auction có đang `OPEN` không
- bid có lớn hơn current price không
- bid có còn trong thời gian cho phép không
- bidder có phải seller hay không

### Vì sao phải thread-safe
Validation này phụ thuộc trực tiếp vào shared state. Nếu validate ngoài lock rồi mới update trong lock, kết quả validate có thể đã hết giá trị.

### Kết luận
Validation phụ thuộc `currentPrice`, `auctionStatus`, `endTime`, `currentLeader` phải nằm trong critical section.

---

## 4.3 Cập nhật current price
Đây là phần chắc chắn phải thread-safe.

### Vì sao
`currentPrice` là dữ liệu trung tâm của auction. Nếu hai thread cùng ghi mà không đồng bộ:
- có thể mất update
- có thể giá cuối không phản ánh đúng thứ tự xử lý
- có thể phát event sai

### Kết luận
Mọi cập nhật vào `currentPrice` phải nằm trong vùng khóa của auction.

---

## 4.4 Cập nhật current leader
### Vì sao phải thread-safe
`currentLeader` phải luôn đồng bộ với `currentPrice`.

Nếu cập nhật leader ngoài lock hoặc tách rời khỏi update giá:
- current price có thể đúng nhưng leader sai
- UI sẽ hiển thị sai người đang dẫn đầu

### Kết luận
`currentPrice` và `currentLeader` phải được cập nhật nguyên tử trong cùng critical section.

---

## 4.5 Ghi bid history
### Vì sao phải thread-safe
Bid history phải phản ánh đúng:
- giá mới
- leader mới
- thứ tự bid
- timestamp server

Nếu state đã đổi nhưng bid history chưa lưu hoặc lưu lệch thứ tự:
- audit khó kiểm chứng
- realtime history có thể sai
- có thể khó giải thích winner cuối cùng

### Kết luận
Ghi `bidHistory` phải được xem là một phần của cùng transaction logic với update state.

---

## 4.6 Xử lý anti-sniping và cập nhật end time
Nếu hệ thống có rule:
- bid hợp lệ đến trong X giây cuối
- thì gia hạn thêm Y giây/phút

thì phần cập nhật `endTime` cũng phải thread-safe.

### Vì sao
Nếu nhiều bid sát giờ đến gần đồng thời:
- có thể gia hạn sai nhiều lần
- có thể ghi đè `endTime`
- countdown trên client có thể lệch

### Kết luận
Nếu bid flow có anti-sniping, `endTime` phải được xử lý trong cùng vùng khóa với bid.

---

## 4.7 Chuyển auction status
Ví dụ:
- từ `OPEN` sang `CLOSED`
- từ `OPEN` sang `EXTENDED`
- từ `OPEN` sang `CANCELLED`

### Vì sao phải thread-safe
Nếu một thread đang đóng auction còn thread khác vẫn nhận bid:
- có thể nhận bid sau khi auction đã đóng
- có thể chốt winner sai
- trạng thái auction có thể không nhất quán

### Kết luận
Các thay đổi `auctionStatus` liên quan trực tiếp đến bid phải được bảo vệ theo auction.

---

## 4.8 Chốt winner
### Vì sao phải thread-safe
Khi auction kết thúc, việc xác định:
- ai là winner
- final price là bao nhiêu

phải dựa trên state cuối cùng đã ổn định.

Nếu đang close auction mà thread khác vẫn chen vào update:
- có thể sai winner
- có thể sai final price
- có thể có hai kết quả chốt khác nhau

### Kết luận
Close auction và chốt winner phải đi qua critical section tương ứng với auction.

---

## 5. Các phần không cần lock sâu cùng mức

## 5.1 Parse request
Ví dụ:
- đọc JSON
- map request sang DTO
- parse `bidAmount`

### Vì sao không cần lock
Phần này không đụng shared state của auction.

---

## 5.2 Validate format input
Ví dụ:
- `bidAmount` có null không
- `bidAmount` có phải số hợp lệ không

### Vì sao không cần lock
Đây là validation cục bộ của request, chưa phụ thuộc state dùng chung.

---

## 5.3 Logging phụ
Ví dụ:
- log request đến
- log debug thông thường

### Vì sao không cần lock
Những log này không ảnh hưởng trực tiếp đến state. Chỉ log audit phục vụ thứ tự xử lý mới cần được thiết kế cẩn thận.

---

## 5.4 Broadcast realtime thực tế
Ví dụ:
- gửi socket event xuống client

### Lưu ý quan trọng
Payload event phải được tạo từ state đã chốt đúng trong vùng lock. Nhưng thao tác gửi event thực tế thường không cần giữ lock.

### Cách làm đúng
- trong lock: chốt state và tạo payload event
- ngoài lock: broadcast payload đó

### Lợi ích
- state vẫn đúng
- lock không bị giữ quá lâu
- hiệu năng tốt hơn

---

## 6. Vùng critical section chuẩn trong bid flow

## 6.1 Ngoài lock
Các bước có thể làm ngoài lock:
- nhận request
- parse request
- validate format cơ bản
- xác thực request ở mức tổng quát

## 6.2 Trong lock theo auctionId
Các bước phải đặt trong vùng khóa của auction:
- đọc `auctionStatus`
- đọc `currentPrice`
- đọc `endTime`
- validate bid theo state hiện tại
- cập nhật `currentPrice`
- cập nhật `currentLeader`
- thêm `bidHistory`
- cập nhật `endTime` nếu anti-sniping
- cập nhật `auctionStatus` nếu cần
- chuẩn bị payload event
- chốt state cuối

## 6.3 Sau lock
Các bước có thể làm sau khi release lock:
- broadcast event
- trả response
- log phụ
- trigger các xử lý không làm đổi shared state trực tiếp

---

## 7. Lock theo auction, không lock toàn hệ thống

## 7.1 Cách đúng: lock per auction
Mỗi auction có một lock riêng:
- auction A có lock riêng
- auction B có lock riêng

### Kết quả
- bid cùng auction được xử lý tuần tự
- bid khác auction vẫn xử lý song song

## 7.2 Cách sai: global lock
Nếu dùng một lock cho toàn bộ hệ thống:
- mọi auction chặn nhau
- hiệu năng giảm mạnh
- không tận dụng được tính song song

### Kết luận
Phase 2 trở đi nên đi theo hướng `lock per auction`.

---

## 8. Pseudo flow đề xuất

```text
Receive Bid Request
    |
    |-- Parse request
    |-- Validate input format
    |
Acquire lock by auctionId
    |
    |-- Read current auction state
    |-- Validate business rules
    |-- Update currentPrice
    |-- Update currentLeader
    |-- Save bidHistory
    |-- Update endTime if anti-sniping
    |-- Build realtime event payload
Release lock
    |
    |-- Broadcast event
    |-- Return response
```

---

## 9. Kịch bản minh họa

## 9.1 Trường hợp đúng với lock
- current price = 100
- user A bid 120
- user B bid 130
- A vào lock trước, cập nhật lên 120
- A release lock
- B vào sau, đọc giá mới là 120
- B cập nhật lên 130
- final state:
  - current price = 130
  - leader = user B

## 9.2 Trường hợp sai nếu không lock
- current price = 100
- A và B cùng đọc 100
- A định nâng 120
- B định nâng 130
- nếu update không đồng bộ:
  - price, leader, history có thể lệch nhau
  - kết quả cuối phụ thuộc vào race condition

---

## 10. Checklist xác định chỗ cần thread-safe
Một đoạn code trong bid flow phải được bảo vệ nếu câu trả lời là “có” cho một trong các câu hỏi sau:

- Có đọc shared state của auction để ra quyết định không?
- Có ghi vào shared state của auction không?
- Nếu hai request chạy cùng lúc ở đây, có thể làm sai kết quả không?
- Đoạn này có thể làm lệch giữa `currentPrice`, `currentLeader`, `bidHistory`, `auctionStatus` không?

Nếu có, đoạn đó nên nằm trong critical section.

---

## 11. Kết luận
Trong bid flow, phần phải thread-safe là:

- đọc state hiện tại của auction để quyết định bid
- validate bid theo state hiện tại
- cập nhật `currentPrice`
- cập nhật `currentLeader`
- ghi `bidHistory`
- cập nhật `endTime` nếu anti-sniping
- cập nhật `auctionStatus`
- chốt `winner`

Các phần như:
- parse request
- validate format input
- log phụ
- broadcast event sau khi state đã chốt

có thể để ngoài lock để hệ thống gọn và hiệu quả hơn.

Nguyên tắc quan trọng nhất là:

> toàn bộ đoạn từ lúc đọc shared state của auction để quyết định bid, cho tới lúc chốt xong state mới của auction, phải được xử lý thread-safe.
