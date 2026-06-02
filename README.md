# AUCTION_PROJECT

> **AUCTION_PROJECT** là hệ thống đấu giá trực tuyến được xây dựng theo mô hình **client-server**. Dự án gồm backend Spring Boot, client desktop JavaFX, cơ chế đấu giá realtime qua WebSocket và cơ sở dữ liệu mặc định H2 file database.

---

## Mục lục

1. [Giới thiệu bài toán](#1-giới-thiệu-bài-toán)
2. [Phạm vi hệ thống](#2-phạm-vi-hệ-thống)
3. [Nhóm người dùng chính](#3-nhóm-người-dùng-chính)
4. [Công nghệ sử dụng](#4-công-nghệ-sử-dụng)
5. [Yêu cầu môi trường](#5-yêu-cầu-môi-trường)
6. [Cấu hình mặc định](#6-cấu-hình-mặc-định)
7. [Biến môi trường hỗ trợ](#7-biến-môi-trường-hỗ-trợ)
8. [Cấu trúc thư mục dự án](#8-cấu-trúc-thư-mục-dự-án)
9. [Hướng dẫn build và kiểm thử](#9-hướng-dẫn-build-và-kiểm-thử)
10. [Hướng dẫn chạy chương trình](#10-hướng-dẫn-chạy-chương-trình)
11. [Tài khoản demo](#11-tài-khoản-demo)
12. [Luồng nghiệp vụ chính](#12-luồng-nghiệp-vụ-chính)
13. [Danh sách chức năng đã hoàn thành](#13-danh-sách-chức-năng-đã-hoàn-thành)
14. [Ghi chú khi test local](#14-ghi-chú-khi-test-local)
15. [Lỗi thường gặp và cách xử lý](#15-lỗi-thường-gặp-và-cách-xử-lý)
16. [Báo cáo và video demo](#16-báo-cáo-và-video-demo)

---

## 1. Giới thiệu bài toán

`AUCTION_PROJECT` là hệ thống đấu giá trực tuyến cho phép người bán đăng sản phẩm, quản trị viên duyệt sản phẩm và tạo phiên đấu giá, còn người mua tham gia đặt giá theo thời gian thực.

Mục tiêu của hệ thống là mô phỏng đầy đủ một quy trình đấu giá cơ bản, bao gồm:

- đăng ký và đăng nhập người dùng;
- phân quyền theo vai trò;
- tạo và quản lý sản phẩm đấu giá;
- duyệt sản phẩm trước khi đưa vào phiên đấu giá;
- đặt giá thủ công;
- hỗ trợ auto-bid;
- cập nhật giá realtime bằng WebSocket;
- quản lý ví người dùng;
- theo dõi người thắng cuộc sau khi phiên đấu giá kết thúc.

Dự án phù hợp để chạy thử nghiệm ở môi trường local, phục vụ học tập, báo cáo môn học hoặc demo quy trình đấu giá end-to-end.

---

## 2. Phạm vi hệ thống

### 2.1. Phạm vi hiện tại

Hệ thống hiện tập trung vào các nghiệp vụ nội bộ của một nền tảng đấu giá trực tuyến:

- xác thực người dùng và phân quyền theo vai trò;
- quản lý listing sản phẩm của seller;
- quản lý phiên đấu giá của admin;
- đặt giá và theo dõi lịch sử bid;
- đấu giá realtime qua WebSocket;
- quản lý ví, số dư và lịch sử giao dịch;
- upload nhiều ảnh cho sản phẩm;
- dashboard thống kê cơ bản cho seller và admin;
- lưu trữ dữ liệu bằng H2 file database mặc định;
- cho phép override sang MySQL hoặc PostgreSQL thông qua biến môi trường.

### 2.2. Những phần chưa nằm trong phạm vi hiện tại

Dự án hiện **chưa bao gồm** các phần sau:

- thanh toán qua bên thứ ba như VNPay, Momo, PayPal hoặc Stripe;
- quy trình vận chuyển sau khi đấu giá thành công;
- lưu trữ ảnh trên cloud object storage như AWS S3, Google Cloud Storage hoặc MinIO;
- ứng dụng web frontend riêng biệt;
- ứng dụng mobile riêng biệt;
- quy trình production deployment hoàn chỉnh;
- hệ thống notification nâng cao qua email, SMS hoặc push notification.

---

## 3. Nhóm người dùng chính

Hệ thống hỗ trợ 3 nhóm người dùng chính: `Bidder`, `Seller` và `Admin`.

### 3.1. Bidder

`Bidder` là người tham gia đấu giá. Các chức năng chính gồm:

- xem danh sách phiên đấu giá;
- xem chi tiết sản phẩm;
- tìm kiếm sản phẩm;
- xem sản phẩm theo category, showroom hoặc trending;
- đặt giá thủ công;
- bật auto-bid;
- theo dõi lịch sử đấu giá;
- thêm phiên đấu giá vào danh sách yêu thích;
- quản lý ví cá nhân;
- xem các phiên đấu giá đã thắng.

### 3.2. Seller

`Seller` là người bán sản phẩm. Các chức năng chính gồm:

- tạo listing sản phẩm;
- chỉnh sửa listing;
- xóa listing;
- upload nhiều ảnh cho một sản phẩm;
- theo dõi trạng thái duyệt sản phẩm;
- quản lý các sản phẩm đang bán;
- xem dashboard thống kê và inventory.

Lưu ý: tài khoản seller không được seed sẵn. Muốn test vai trò seller, cần đăng ký mới từ màn hình `Register`.

### 3.3. Admin

`Admin` là quản trị viên hệ thống. Các chức năng chính gồm:

- duyệt listing do seller gửi lên;
- tạo phiên đấu giá từ item đã được duyệt;
- quản lý vòng đời phiên đấu giá;
- theo dõi dashboard hệ thống;
- xem recent notifications;
- xem hoạt động ví và giao dịch;
- theo dõi trạng thái winner và reserve price.

---

## 4. Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.2.5 |
| Web/API | Spring Web |
| ORM/Database | Spring Data JPA |
| Security | Spring Security, JWT |
| Validation | Spring Validation |
| Realtime | Spring WebSocket, STOMP, SockJS |
| Desktop client | JavaFX 21.0.2 |
| HTTP client | Java HttpClient |
| JSON | Jackson, Gson |
| Database mặc định | H2 file database |
| Database tùy chọn | MySQL, PostgreSQL |
| Test | JUnit 5, Mockito, Spring Boot Test, Testcontainers |
| Build tool | Maven multi-module |

### 4.1. Backend

Backend được xây dựng bằng Spring Boot, đảm nhận các nhiệm vụ:

- cung cấp REST API cho client;
- xử lý xác thực và phân quyền;
- quản lý business logic;
- truy xuất dữ liệu thông qua JPA repository;
- phát sự kiện realtime cho client qua WebSocket;
- lưu thông tin sản phẩm, người dùng, phiên đấu giá, bid và ví.

### 4.2. Client desktop

Client được xây dựng bằng JavaFX, có nhiệm vụ:

- hiển thị giao diện người dùng;
- gọi API backend;
- quản lý session đăng nhập cục bộ;
- nhận sự kiện realtime từ WebSocket;
- điều hướng giữa các màn hình như login, register, showroom, product detail, dashboard, wallet.

### 4.3. Realtime bidding

Realtime bidding sử dụng WebSocket với STOMP/SockJS để cập nhật thông tin đấu giá gần như tức thời. Khi có người dùng đặt giá mới, backend phát event để client cập nhật lại giá hiện tại, lịch sử bid hoặc trạng thái phiên đấu giá.

---

## 5. Yêu cầu môi trường

Trước khi chạy dự án, cần cài đặt các thành phần sau:

- `JDK 21`
- `Maven 3.9+`
- hệ điều hành Windows, Linux hoặc macOS;
- môi trường có giao diện đồ họa nếu muốn chạy JavaFX client.

### 5.1. Kiểm tra Java

Windows, Linux hoặc macOS:

```bash
java -version
```

Kết quả cần hiển thị Java 21, ví dụ:

```text
java version "21.x.x"
```

Nếu kết quả là Java 17, Java 11 hoặc phiên bản khác, cần chỉnh lại `JAVA_HOME` trỏ về JDK 21.

### 5.2. Kiểm tra Maven

Windows, Linux hoặc macOS:

```bash
mvn -version
```

Kết quả cần thỏa mãn:

- Maven từ 3.9 trở lên;
- Java runtime đang dùng là JDK 21.

---

## 6. Cấu hình mặc định

### 6.1. Backend

Backend mặc định chạy với cấu hình sau:

| Thuộc tính | Giá trị mặc định |
| --- | --- |
| Host | `0.0.0.0` |
| Port | `8081` |
| Database URL | `jdbc:h2:file:./data/auctiondb;MODE=PostgreSQL` |
| Upload directory | `uploads` |

Sau khi chạy backend, server sẽ lắng nghe ở địa chỉ:

```text
http://localhost:8081
```

### 6.2. Client

Client mặc định lấy cấu hình endpoint từ file:

```text
client/src/main/resources/auction-client.properties
```

Trong source hiện tại, client có thể đang trỏ tới server public. Khi muốn test local, không cần sửa source code trực tiếp. Có thể override endpoint bằng biến môi trường hoặc JVM property.

---

## 7. Biến môi trường hỗ trợ

### 7.1. Biến môi trường backend

| Biến môi trường | Ý nghĩa |
| --- | --- |
| `AUCTION_SERVER_ADDRESS` | Địa chỉ bind của backend server |
| `AUCTION_SERVER_PORT` | Port chạy backend |
| `AUCTION_DB_URL` | JDBC URL của database |
| `AUCTION_DB_USERNAME` | Username database |
| `AUCTION_DB_PASSWORD` | Password database |
| `AUCTION_DB_DRIVER` | Driver database |
| `AUCTION_UPLOAD_DIR` | Thư mục lưu file upload |
| `AUCTION_JWT_SECRET` | Secret dùng để ký JWT |
| `AUCTION_SHOW_SQL` | Bật/tắt hiển thị SQL log |

Ví dụ cấu hình backend dùng database mặc định:

```bash
mvn -f server/pom.xml spring-boot:run
```

Ví dụ override port backend:

```bash
AUCTION_SERVER_PORT=8081 mvn -f server/pom.xml spring-boot:run
```

### 7.2. Biến môi trường client

| Biến môi trường / JVM property | Ý nghĩa |
| --- | --- |
| `AUCTION_API_BASE_URL` | Base URL REST API |
| `AUCTION_WS_URL` | WebSocket URL |
| `auction.api.baseUrl` | JVM property override REST API |
| `auction.ws.url` | JVM property override WebSocket |

Khi chạy JavaFX qua Maven, cách ổn định nhất là override bằng biến môi trường.

---

## 8. Cấu trúc thư mục dự án

```text
AUCTION_PROJECT/
|-- pom.xml                         # Maven aggregator root
|-- README.md                       # Tài liệu hướng dẫn dự án
|-- TEST_FLOWS.md                   # Tài liệu mô tả luồng test
|-- checkstyle.xml                  # Cấu hình kiểm tra style code
|-- run-local-server.bat            # Script chạy backend local trên Windows
|-- run-local-client.bat            # Script chạy client local trên Windows
|-- run-local-all.bat               # Script mở cả server và client trên Windows
|-- server/
|   |-- pom.xml                     # Maven config của backend
|   |-- mvnw                        # Maven wrapper cho Linux/macOS
|   |-- mvnw.cmd                    # Maven wrapper cho Windows
|   `-- src/
|       |-- main/java/com/auction/server/
|       |   |-- controller/         # REST API: auth, auction, admin, seller, wallet...
|       |   |-- service/            # Business logic
|       |   |-- repository/         # JPA repositories
|       |   |-- entity/             # Entity ánh xạ database
|       |   |-- config/             # Security, JWT, WebSocket, seed account
|       |   |-- realtime/           # Publish sự kiện realtime
|       |   `-- scheduler/          # Tiện ích cập nhật trạng thái auction
|       `-- test/                   # Unit test và integration test
`-- client/
    |-- pom.xml                     # Maven config của JavaFX client
    `-- src/
        |-- main/java/com/auction/client/
        |   |-- controller/         # JavaFX controllers
        |   |-- service/            # API services, socket services
        |   |-- dto/                # Request, response, event DTO
        |   |-- navigation/         # Điều hướng scene
        |   |-- session/            # Quản lý session local
        |   `-- config/             # EndpointConfig và cấu hình client
        `-- main/resources/
            |-- fxml/               # JavaFX layout files
            |-- css/                # File giao diện
            `-- images/             # Tài nguyên hình ảnh
```

### 8.1. Module `server`

Module `server` chứa toàn bộ backend Spring Boot. Đây là nơi xử lý API, nghiệp vụ đấu giá, bảo mật, lưu trữ dữ liệu và gửi sự kiện realtime.

Một số package quan trọng:

- `controller`: định nghĩa các REST API cho auth, admin, seller, bidder, auction và wallet;
- `service`: xử lý nghiệp vụ chính của hệ thống;
- `repository`: giao tiếp với database thông qua Spring Data JPA;
- `entity`: định nghĩa các bảng dữ liệu;
- `config`: cấu hình bảo mật, JWT, WebSocket và seed account;
- `realtime`: phát event realtime cho client;
- `scheduler`: cập nhật trạng thái phiên đấu giá theo thời gian.

### 8.2. Module `client`

Module `client` là ứng dụng desktop JavaFX. Đây là phần người dùng tương tác trực tiếp.

Một số package quan trọng:

- `controller`: xử lý logic giao diện JavaFX;
- `service`: gọi REST API và kết nối WebSocket;
- `dto`: chứa object request, response và realtime event;
- `navigation`: điều hướng giữa các màn hình;
- `session`: lưu thông tin đăng nhập cục bộ;
- `config`: cấu hình endpoint backend.

---

## 9. Hướng dẫn build và kiểm thử

Tất cả lệnh bên dưới có thể chạy trên Windows, Linux và macOS. Nếu dùng Windows, có thể thay `/` bằng `\` trong đường dẫn Maven nếu cần.

### 9.1. Build và verify backend

Windows:

```cmd
mvn -f server\pom.xml clean verify -Dspring.profiles.active=test
```

Linux / macOS:

```bash
mvn -f server/pom.xml clean verify -Dspring.profiles.active=test
```

Lệnh này sẽ:

- clean project backend;
- compile source code;
- chạy unit test;
- chạy integration test nếu được cấu hình;
- kiểm tra project có build thành công hay không.

### 9.2. Build và verify client

Windows:

```cmd
mvn -f client\pom.xml clean verify
```

Linux / macOS:

```bash
mvn -f client/pom.xml clean verify
```

Lệnh này dùng để kiểm tra phần JavaFX client có compile và verify thành công hay không.

---

## 10. Hướng dẫn chạy chương trình

Để chạy đầy đủ hệ thống ở local, nên chạy backend trước, sau đó chạy client.

---

### 10.1. Chạy backend

#### Windows

```cmd
mvn -f server\pom.xml spring-boot:run
```

#### Linux / macOS

```bash
mvn -f server/pom.xml spring-boot:run
```

Khi backend chạy thành công, terminal sẽ xuất hiện log tương tự:

```text
Tomcat started on port 8081
Started AuctionServerApplication
```

Lưu ý: nếu truy cập `GET /api/hello` và nhận `403 Forbidden`, điều này không nhất thiết là lỗi. Endpoint này có thể không phải endpoint public do cấu hình security.

---

### 10.2. Chạy client để test local mà không sửa source

Khi backend đang chạy ở local tại `http://localhost:8081`, cần override endpoint client về local.

#### Windows CMD

```cmd
set AUCTION_API_BASE_URL=http://localhost:8081
set AUCTION_WS_URL=ws://localhost:8081/ws/websocket
mvn -f client\pom.xml clean javafx:run
```

#### Windows PowerShell

```powershell
$env:AUCTION_API_BASE_URL="http://localhost:8081"
$env:AUCTION_WS_URL="ws://localhost:8081/ws/websocket"
mvn -f client/pom.xml clean javafx:run
```

#### Linux / macOS

```bash
AUCTION_API_BASE_URL=http://localhost:8081 \
AUCTION_WS_URL=ws://localhost:8081/ws/websocket \
mvn -f client/pom.xml clean javafx:run
```

---

## 11. Tài khoản demo

Hệ thống seed sẵn một số tài khoản demo cho admin và bidder:

| Vai trò | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `Admin@123` |
| Admin | `admin1` | `Admin@123` |
| Admin | `admin2` | `Admin@456` |
| Bidder | `bidder` | `123456` |
| Bidder | `bidder1` | `123456` |

Lưu ý:

- tài khoản `seller` không được seed sẵn;
- muốn test seller thì đăng ký tài khoản mới từ màn hình `Register`;
- khi đăng nhập, vai trò được chọn trên màn hình login cần khớp với vai trò thật của tài khoản.

---

## 12. Luồng nghiệp vụ chính

### 12.1. Luồng đăng ký và đăng nhập

1. Người dùng mở client JavaFX.
2. Chọn đăng ký tài khoản mới hoặc đăng nhập bằng tài khoản có sẵn.
3. Backend kiểm tra thông tin đăng nhập.
4. Nếu hợp lệ, backend trả về token/session tương ứng.
5. Client lưu session cục bộ và điều hướng sang màn hình phù hợp với vai trò.

### 12.2. Luồng seller tạo sản phẩm

1. Seller đăng nhập hệ thống.
2. Seller tạo listing mới.
3. Seller nhập thông tin sản phẩm.
4. Seller upload một hoặc nhiều ảnh.
5. Listing được gửi lên backend.
6. Listing chờ admin duyệt.
7. Seller theo dõi trạng thái listing trong dashboard.

### 12.3. Luồng admin duyệt listing và tạo auction

1. Admin đăng nhập hệ thống.
2. Admin xem danh sách listing đang chờ duyệt.
3. Admin kiểm tra thông tin sản phẩm.
4. Admin duyệt listing hợp lệ.
5. Admin tạo phiên đấu giá từ item đã duyệt.
6. Admin quản lý trạng thái và vòng đời phiên đấu giá.

### 12.4. Luồng bidder tham gia đấu giá

1. Bidder đăng nhập hệ thống.
2. Bidder xem danh sách phiên đấu giá đang mở.
3. Bidder mở chi tiết sản phẩm.
4. Bidder xem giá hiện tại và lịch sử bid.
5. Bidder đặt giá thủ công hoặc bật auto-bid.
6. Backend kiểm tra giá hợp lệ và số dư ví nếu có ràng buộc.
7. Backend lưu bid mới.
8. Backend phát sự kiện realtime qua WebSocket.
9. Các client đang theo dõi phiên đấu giá nhận cập nhật giá mới.
10. Theo dõi được biểu đồ theo thời gian thực 

### 12.5. Luồng kết thúc phiên đấu giá

1. Scheduler hoặc logic backend cập nhật trạng thái phiên đấu giá.
2. Khi phiên đấu giá kết thúc, hệ thống xác định người thắng.
3. Hệ thống kiểm tra điều kiện reserve price nếu có.
4. Bidder thắng có thể xem phiên đấu giá trong màn hình `Won Auctions`.
5. Admin và seller có thể theo dõi kết quả trong dashboard tương ứng.

---

## 13. Danh sách chức năng đã hoàn thành

### 13.1. Xác thực và phân quyền

- Đăng ký tài khoản.
- Đăng nhập bằng username hoặc email.
- Phân quyền theo vai trò `ADMIN`, `SELLER`, `BIDDER`.
- Seed sẵn tài khoản demo cho admin và bidder.

### 13.2. Chức năng seller

- Tạo listing sản phẩm.
- Sửa listing.
- Xóa listing.
- Upload nhiều ảnh cho item.
- Theo dõi trạng thái duyệt sản phẩm.
- Xem dashboard seller.
- Theo dõi thống kê và inventory.

### 13.3. Chức năng admin

- Duyệt listing.
- Tạo auction từ item đã duyệt.
- Quản lý vòng đời auction.
- Xem dashboard admin.
- Xem recent notifications.
- Theo dõi wallet activity.
- Theo dõi winner và reserve price.

### 13.4. Chức năng bidder

- Xem showroom.
- Xem danh sách auction theo category.
- Xem trending auctions.
- Tìm kiếm sản phẩm.
- Load more danh sách sản phẩm.
- Xem chi tiết sản phẩm.
- Xem lịch sử bid.
- Đặt giá thủ công.
- Bật auto-bid.
- Thêm auction vào wishlist/favorite.
- Xem màn hình `Won Auctions`.

### 13.5. Chức năng ví

- Xem số dư ví.
- Nạp tiền.
- Rút tiền.
- Xem lịch sử giao dịch.

### 13.6. Chức năng realtime

- Realtime bidding qua WebSocket.
- Cập nhật giá mới cho các client đang theo dõi.
- Hỗ trợ anti-sniping khi có bid sát thời điểm kết thúc.

### 13.7. Kiểm thử

- Có bộ test backend.
- Hỗ trợ unit test.
- Hỗ trợ integration test.
- Sử dụng JUnit 5, Mockito, Spring Boot Test và Testcontainers.

---

## 14. Ghi chú khi test local

### 14.1. Thứ tự chạy khuyến nghị

Nên chạy theo thứ tự sau:

1. Kiểm tra JDK 21.
2. Kiểm tra Maven.
3. Chạy backend.
4. Chờ backend start xong.
5. Chạy client với endpoint local.
6. Đăng nhập bằng tài khoản demo hoặc đăng ký tài khoản mới.
7. Test luồng admin, seller và bidder.

### 14.2. Cách kiểm tra backend đã sẵn sàng

Backend được xem là sẵn sàng khi log có dòng tương tự:

```text
Started AuctionServerApplication
```

Nếu client báo lỗi kết nối, hãy kiểm tra:

- backend đã chạy chưa;
- backend có chạy đúng port `8081` không;
- biến `AUCTION_API_BASE_URL` đã trỏ đúng `http://localhost:8081` chưa;
- biến `AUCTION_WS_URL` đã trỏ đúng `ws://localhost:8081/ws/websocket` chưa.

## 15. Link báo cáo PDF và video demo

Cần thay các placeholder dưới đây bằng link thật trước khi nộp bài:

- Báo cáo PDF: https://drive.google.com/file/d/1fZvMUNnufzo_n4_qBof3fe40M_IIAZw0/view?usp=sharing
- Video demo: https://drive.google.com/file/d/1SKrgVcBNg-Bd9XskpHlDGq_5HCOJzp23/view?usp=sharing

---

