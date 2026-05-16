# Ke hoach cong viec cho nhom 4 nguoi

File nay dung de chia viec theo rubric cham diem cua project dau gia. Muc tieu truoc mat la dua project ve trang thai build/test duoc, sau do hoan thien cac muc bat buoc: dau gia hop le, xu ly dong thoi, realtime update, client-server, MVC, CI/CD va unit test.

## Nguyen tac lam viec chung

- Moi nguoi lam tren branch rieng, dat ten theo mau: `feature/<ten-viec>`.
- Truoc khi code moi phai keo code moi nhat tu branch chung cua nhom.
- Khong sua lan file cua nguoi khac neu chua thong bao.
- Moi task xong can co bang chung: anh man hinh, ket qua test, log API, hoac video demo ngan.
- Uu tien muc bat buoc truoc, optional lam sau khi project da build va demo on dinh.

## Trang thai hien tai can luu y

- Backend dang co loi build khi chay `.\mvnw.cmd test` tren may dang dung Java 25. Project backend cau hinh Java 17 va Lombok 1.18.30, nen can chay bang JDK 17 hoac nang Lombok.
- Backend co WebSocket config nhung chua publish event sau khi co bid moi.
- UI realtime hien moi la skeleton/simulate, chua ket noi STOMP/WebSocket that.
- Backend co `@Scheduled` nhung app chinh chua bat `@EnableScheduling`.
- Security dang `permitAll` gan nhu toan bo endpoint, chua co auth/role enforcement thuc su.
- Unit test hien con mong, chu yeu moi co test cho `BidServiceImpl`.

---

# 1. Nguoi Backend

## Pham vi chinh

Phu trach REST API, entity, service, repository, validation nghiep vu, authentication/authorization va exception handling.

## Viec can lam uu tien cao

### 1.1 Sua build backend

Muc tieu: backend chay duoc `.\mvnw.cmd test`.

Viec can lam:

- Dam bao backend dung JDK 17 khi build.
- Neu nhom muon dung JDK moi hon, cap nhat Lombok trong `backend/pom.xml` len ban moi hon.
- Kiem tra lai Maven compiler config.
- Chay:

```powershell
cd backend
.\mvnw.cmd clean test
```

Tieu chi xong:

- Maven build thanh cong.
- Tat ca test backend pass.
- Ghi lai version Java da dung vao README hoac file huong dan chay.

### 1.2 Hoan thien API dau gia

File lien quan:

- `backend/src/main/java/com/team/backend/controller/AuctionController.java`
- `backend/src/main/java/com/team/backend/service/impl/AuctionServiceImpl.java`
- `backend/src/main/java/com/team/backend/service/impl/BidServiceImpl.java`
- `backend/src/main/java/com/team/backend/repository/AuctionRepository.java`
- `backend/src/main/java/com/team/backend/repository/BidRepository.java`

Viec can lam:

- Dam bao endpoint dat gia `POST /api/auctions/{id}/bids` hoat dong on dinh.
- Kiem tra cac dieu kien:
  - Auction ton tai.
  - Auction dang ACTIVE.
  - Chua het thoi gian.
  - Gia dat phai lon hon hoac bang `currentPrice + minIncrement`.
  - Nguoi dat gia hop le.
- Sau khi dat gia thanh cong:
  - Cap nhat `currentPrice`.
  - Cap nhat `leaderId`.
  - Luu `BidTransaction`.
  - Tra ve auction moi nhat.

Tieu chi xong:

- Test duoc dat gia thanh cong bang Postman/curl.
- Dat gia thap hon bi loi 400.
- Auction het han bi loi dung nghia.
- API lich su bid tra ve dung danh sach.

### 1.3 Sua scheduler trang thai auction

File lien quan:

- `backend/src/main/java/com/team/backend/BackendApplication.java`
- `backend/src/main/java/com/team/backend/scheduler/AuctionStateScheduler.java`
- `backend/src/main/java/com/team/backend/service/impl/AuctionServiceImpl.java`

Viec can lam:

- Them `@EnableScheduling` vao `BackendApplication`.
- Tranh trung logic scheduler neu ca `AuctionStateScheduler` va `AuctionServiceImpl` cung co `@Scheduled`.
- Chon mot noi lam scheduler chinh.
- Khi auction ket thuc:
  - Set state = `FINISHED`.
  - Set `winnerId = leaderId`.

Tieu chi xong:

- Auction co `startTime` trong qua khu tu chuyen sang ACTIVE.
- Auction qua `endTime` tu chuyen sang FINISHED.
- Winner duoc gan dung.

### 1.4 Sua security

File lien quan:

- `backend/src/main/java/com/team/backend/config/SecurityConfig.java`
- `backend/src/main/java/com/team/backend/controller/AuthController.java`
- `backend/src/main/java/com/team/backend/service/impl/UserServiceImpl.java`

Viec can lam:

- Bat method security bang `@EnableMethodSecurity`.
- Khong de `/api/**`, `/seller/**`, `/admin/**` permitAll het.
- Public:
  - `/api/auth/**`
  - `/ws/**` neu dung WebSocket public handshake.
- Bao ve:
  - `/admin/**`: role ADMIN.
  - `/seller/**`: role SELLER.
  - endpoint dat gia: user da login.
- Neu co thoi gian: lam JWT token that.
- Neu khong kip JWT: toi thieu phai co co che xac thuc ro rang va khong dua vao header user id tuy tien trong ban demo cuoi.

Tieu chi xong:

- User thuong khong goi duoc API admin.
- Seller khong sua san pham cua seller khac.
- Bidder dat gia duoc khi login.

## Viec bo sung neu con thoi gian

- Chuan hoa DTO response, khong tra entity JPA truc tiep cho tat ca API.
- Them pagination/filter cho danh sach auctions.
- Viet README backend API.

---

# 2. Nguoi Server / DevOps / Integration

## Pham vi chinh

Phu trach moi truong chay, database, CI/CD, cau hinh port, application properties, script build va tai lieu deploy/demo.

## Viec can lam uu tien cao

### 2.1 Chuan hoa moi truong chay

File lien quan:

- `backend/src/main/resources/application.properties`
- `backend/src/test/resources/application-test.properties`
- `backend/pom.xml`
- `ui/pom.xml`
- `README.md`

Viec can lam:

- Ghi ro version:
  - Java backend: JDK 17.
  - Java UI: JDK 21 neu giu JavaFX 21, hoac dong bo lai neu muon.
  - Maven wrapper dung cho backend.
- Kiem tra port backend dang chay, hien UI goi `http://localhost:8081`.
- Dam bao `application.properties` backend dung port 8081 neu UI hard-code 8081.
- Viet huong dan chay:
  - Chay MySQL.
  - Chay backend.
  - Chay UI.

Tieu chi xong:

- May thanh vien khac lam theo README va chay duoc.
- Khong can doan port hay database name.

### 2.2 Sua CI/CD GitHub Actions

File lien quan:

- `.github/workflows/backend-ci.yml`
- Tao them `.github/workflows/ui-ci.yml` neu can.

Viec can lam:

- Dam bao backend CI dung JDK 17.
- Chay `cd backend && ./mvnw test`.
- Them cache Maven neu can.
- Them job build UI:
  - Setup JDK 21.
  - `cd ui && mvn test` hoac `mvn -DskipTests package`.
- Neu UI khong co test, toi thieu build compile.

Tieu chi xong:

- Push len GitHub Actions pass.
- Co badge hoac anh chup CI pass de dua vao bao cao.

### 2.3 Database va du lieu demo

Viec can lam:

- Chuan hoa database `auction_db`.
- Co script tao du lieu demo hoac initializer:
  - 1 admin.
  - 1 seller.
  - 2 bidder.
  - 3 san pham.
  - 2 auction dang active.
- Dam bao password demo duoc ghi trong README.

Tieu chi xong:

- Demo khong can tao tay qua DB.
- Restart app van co tai khoan demo can thiet.

### 2.4 Tai lieu demo

Tao/cap nhat:

- `README.md`
- `DEMO_SCRIPT.md` neu can.

Noi dung can co:

- Cach build backend.
- Cach build UI.
- Cach chay.
- Tai khoan demo.
- Flow demo:
  - Register/login.
  - Seller tao san pham.
  - Admin approve.
  - Tao auction.
  - Bidder dat gia.
  - Realtime cap nhat.
  - Auction ket thuc.

---

# 3. Nguoi UI

## Pham vi chinh

Phu trach JavaFX UI, FXML, CSS, navigation, API client, hien thi realtime, bid history va validation phia client.

## Viec can lam uu tien cao

### 3.1 Ket noi UI voi backend that

File lien quan:

- `ui/src/main/java/com/auction/client/service/ApiClient.java`
- `ui/src/main/java/com/auction/client/service/AuctionApiService.java`
- `ui/src/main/java/com/auction/client/service/AuthApiService.java`
- `ui/src/main/java/com/auction/client/service/SellerItemApiService.java`
- `ui/src/main/java/com/auction/client/service/AdminApiService.java`

Viec can lam:

- Kiem tra tat ca endpoint UI goi co khop backend khong.
- Giam phu thuoc `MockData` neu man hinh da co API that.
- Xu ly loi API than thien:
  - Server khong chay.
  - 401/403.
  - 400 validation.
  - Auction het han.

Tieu chi xong:

- UI login/register duoc.
- UI load danh sach auction tu backend.
- UI dat gia thanh cong qua backend.

### 3.2 Hoan thien man hinh live bidding

File lien quan:

- `ui/src/main/java/com/auction/client/controller/LiveBiddingController.java`
- `ui/src/main/resources/fxml/live_bidding.fxml`
- `ui/src/main/resources/css/live_bidding.css`

Viec can lam:

- Hien thi:
  - Ten san pham.
  - Gia hien tai.
  - Nguoi dang dan dau.
  - Countdown.
  - Trang thai ket noi.
  - Lich su bid.
- Disable nut bid khi:
  - Chua login.
  - Auction FINISHED.
  - Gia nhap khong hop le.
  - Dang gui request.
- Sau khi bid thanh cong:
  - Clear input.
  - Cap nhat gia.
  - Them lich su bid.

Tieu chi xong:

- Demo 2 user dat gia thay doi man hinh dung.
- Khong bi crash khi backend tra loi loi.

### 3.3 Lam WebSocket client that

File lien quan:

- `ui/src/main/java/com/auction/client/socket/AuctionSocketClient.java`
- `ui/src/main/java/com/auction/client/service/RealtimeAuctionService.java`

Viec can lam:

- Thay skeleton `connected = true` bang ket noi WebSocket/STOMP that.
- Subscribe topic:

```text
/topic/auctions/{auctionId}
```

- Parse event backend gui ve:
  - `BID_PLACED`
  - `LEADER_CHANGED`
  - `AUCTION_FINISHED`
  - `AUCTION_EXTENDED`
- Neu WebSocket loi, UI van co polling fallback 5 giay.

Tieu chi xong:

- Khi user A dat gia, user B thay gia cap nhat ma khong can bam refresh.
- Trang thai socket hien dung: CONNECTED, DISCONNECTED, RECONNECTING neu co.

### 3.4 Bid history visualization

Viec can lam:

- Goi API `GET /api/auctions/{id}/bids`.
- Hien thi lich su bid that tu server.
- Neu can visualization:
  - List lich su co timestamp.
  - Them chart don gian gia theo thoi gian neu kip.

Tieu chi xong:

- Reload man hinh van thay lich su bid cu.
- Lich su khong chi la local list trong UI.

---

# 4. Nguoi Hardcore / Core Logic

## Pham vi chinh

Phu trach cac phan kho va de mat diem: concurrency, realtime event flow, anti-sniping, auto-bidding, test nang cao va design pattern.

## Viec can lam uu tien cao

### 4.1 Kiem tra va gia co concurrency bidding

File lien quan:

- `backend/src/main/java/com/team/backend/service/impl/BidServiceImpl.java`
- `backend/src/main/java/com/team/backend/repository/AuctionRepository.java`
- `backend/src/test/java/com/team/backend/service/impl/BidServiceImplTest.java`

Viec can lam:

- Kiem tra lock pessimistic co hoat dong voi MySQL.
- Khong de 2 bid cung luc cung thang sai.
- Dam bao transaction bao tron:
  - Load auction with lock.
  - Validate.
  - Update auction.
  - Save bid transaction.
- Can nhac them `@Version` vao `Auction` neu muon optimistic locking.
- Viet test concurrency bang `ExecutorService` hoac integration test voi H2/MySQL.

Tieu chi xong:

- Test 10 request dong thoi vao cung auction van chi chap nhan bid hop le.
- `currentPrice`, `leaderId`, `BidTransaction` nhat quan.

### 4.2 Thiet ke realtime event flow

Viec can lam:

- Tao DTO event rieng, vi du:
  - `AuctionEventDto`
  - `BidPlacedEvent`
  - `AuctionFinishedEvent`
- Sau khi bid thanh cong, backend publish event.
- Sau khi auction finish/extend, backend publish event.
- Khong publish event khi transaction fail.

Flow mong muon:

```text
UI place bid
-> REST POST /api/auctions/{id}/bids
-> BidService validate + save transaction
-> Backend publish /topic/auctions/{id}
-> Tat ca UI dang xem auction nhan event
-> UI cap nhat current price/history/leader
```

Tieu chi xong:

- Khong bi duplicate event.
- UI nhan dung event sau khi bid.
- Neu REST fail thi khong co realtime event gia.

### 4.3 Anti-sniping

Muc tieu: neu co bid sat gio ket thuc, auction duoc gia han de tranh dat gia vao giay cuoi.

De xuat rule:

- Neu bid duoc dat trong 60 giay cuoi.
- Gia han auction them 60 giay.
- Chi gia han toi da N lan neu muon tranh keo dai vo han.

Viec can lam:

- Them config:

```properties
auction.anti-sniping.enabled=true
auction.anti-sniping.threshold-seconds=60
auction.anti-sniping.extend-seconds=60
```

- Trong `BidServiceImpl`, sau khi bid hop le:
  - Tinh remaining time.
  - Neu remaining <= threshold, cap nhat `endTime`.
  - Publish event `AUCTION_EXTENDED`.

Tieu chi xong:

- Bid trong 60 giay cuoi lam countdown tang them.
- UI nhan event va cap nhat countdown.
- Co unit test.

### 4.4 Auto-bidding

Muc tieu: user dat gia toi da, he thong tu dong tang bid theo buoc toi thieu khi co doi thu dat gia.

De xuat entity:

- `AutoBid`
  - `id`
  - `auctionId`
  - `bidderId`
  - `maxAmount`
  - `active`
  - `createdAt`

API de xuat:

- `POST /api/auctions/{id}/auto-bids`
- `GET /api/auctions/{id}/auto-bids/me`
- `DELETE /api/auctions/{id}/auto-bids/me`

Logic de xuat:

- Khi user A dat auto max = 1000.
- Gia hien tai = 500.
- User B bid = 600.
- System tu dat gia cho A = 601 hoac `B + minIncrement`, neu <= 1000.
- Neu B bid > 1000, A bi outbid.

Tieu chi xong:

- Khong tao loop auto-bid vo han.
- Khong cho user tu auto-bid canh tranh voi chinh minh.
- Co test cho 2 user auto-bid.

### 4.5 Design pattern va clean architecture

Viec can lam:

- Giu MVC ro:
  - Controller chi nhan request/tra response.
  - Service chua business logic.
  - Repository chi truy van DB.
- Ap dung pattern co y nghia:
  - Observer/Event publishing cho realtime.
  - Strategy cho bid rule neu lam anti-sniping/auto-bidding.
  - Factory/Mapper cho DTO neu can.

Tieu chi xong:

- Bao cao giai thich duoc pattern nao dung o dau.
- Code khong nhung het logic vao controller.

---

# Ke hoach lam theo ngay

## Ngay 1: On dinh build va chay duoc project

- Backend: sua build, chay test pass.
- Server: chuan hoa JDK, port, database, README chay local.
- UI: chay UI, kiem tra endpoint dang goi.
- Hardcore: review logic bid/concurrency, viet danh sach test can co.

Ket qua can co:

- Backend start duoc.
- UI start duoc.
- Login/register/demo data co the dung.
- CI backend pass hoac gan pass.

## Ngay 2: Hoan thien muc bat buoc

- Backend: scheduler, security, API bid/history.
- Server: CI backend + UI build.
- UI: live bidding man hinh hoan thien, polling fallback on dinh.
- Hardcore: concurrency test, realtime event design.

Ket qua can co:

- Dat gia hop le.
- Dau gia tu ket thuc.
- Lich su bid luu DB.
- Test co them case quan trong.

## Ngay 3: Realtime va optional

- Backend + Hardcore: publish WebSocket event.
- UI: subscribe WebSocket that.
- Hardcore: anti-sniping.
- Server: script demo, tai khoan demo.

Ket qua can co:

- 2 UI cung xem 1 auction nhan update realtime.
- Anti-sniping demo duoc.
- README/demo script day du.

## Ngay 4: Optional va polish

- Hardcore: auto-bidding neu con thoi gian.
- UI: bid history visualization/chart.
- Backend: cleanup DTO, validate loi.
- Server: final CI, dong goi huong dan nop bai.

Ket qua can co:

- Demo tron flow.
- Co anh/video minh chung.
- Bao cao noi ro cac muc rubric da dat.

---

# Checklist theo rubric

## Bat buoc

- [ ] Thiet ke lop va cay ke thua ro rang.
- [ ] OOP: encapsulation, inheritance, polymorphism, abstraction.
- [ ] Design pattern co giai thich duoc.
- [ ] Quan ly nguoi dung.
- [ ] Quan ly san pham.
- [ ] Chuc nang dau gia.
- [ ] Xu ly loi va ngoai le.
- [ ] Xu ly dau gia dong thoi.
- [ ] Realtime update bang Observer/WebSocket.
- [ ] Kien truc client-server.
- [ ] MVC: JavaFX + FXML + Controller-Model-DAO/Repository.
- [ ] Maven/Gradle va coding convention.
- [ ] Unit test JUnit.
- [ ] CI/CD GitHub Actions.

## Tuy chon

- [ ] Auto-bidding.
- [ ] Anti-sniping.
- [ ] Bid history visualization.

---

# Thu tu uu tien neu thoi gian gap

1. Sua backend build/test.
2. Bat scheduling.
3. Dam bao bid API dung va co history.
4. Them concurrency test.
5. Lam realtime WebSocket that.
6. Sua security toi thieu theo role.
7. CI backend pass.
8. UI build pass.
9. Anti-sniping.
10. Auto-bidding.
11. Bid history visualization.

