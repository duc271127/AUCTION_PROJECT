# Prototype Result - Thread-safe Bidding

## Mục tiêu
Prototype này mô phỏng 2 thread cùng đặt giá vào 1 auction để kiểm tra cơ chế lock.

## Mô tả
- Auction ban đầu có:
  - auctionId = 1
  - currentPrice = 100.00
  - leader = No Leader yet

- Có 2 thread:
  - Bid-Thread-A: UserA bid 120
  - Bid-Thread-B: UserB bid 130

## Kết quả chạy
- UserA vào critical section trước
- UserA acquire lock và cập nhật giá từ 100 lên 120
- UserA release lock
- UserB vào sau, đọc giá mới là 120
- UserB cập nhật giá từ 120 lên 130
- Final state:
  - currentPrice = 130.00
  - leader = UserB

## Kết luận
Prototype chứng minh rằng:
- cơ chế lock giúp nhiều thread không cùng sửa shared state một lúc
- current price được cập nhật tuần tự
- leader cuối cùng là hợp lệ
- không xảy ra tình trạng 2 người cùng thắng
- đây là nền tảng để phát triển concurrent bidding thật ở phase sau