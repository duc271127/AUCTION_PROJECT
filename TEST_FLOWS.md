# Test flows cho AUCTION_PROJECT

Muc tieu: bo flow nay de test gan nhu toan bo chuc nang hien co cua he thong theo dung mat bang code hien tai, gom client JavaFX va cac API server lien quan.

## 1. Chuan bi moi truong

- Server mac dinh chay local o `http://localhost:8081`.
- Client hien dang tro toi server trong [client/src/main/resources/auction-client.properties](E:/HOC%20UET/FINAL%20BTL/AUCTION_PROJECT/client/src/main/resources/auction-client.properties): `http://lungs-decree.with.playit.plus:1125`.
- Neu ban muon test local-end-to-end, sua `auction.api.baseUrl=http://localhost:8081` roi chay:
  - `mvn -pl server spring-boot:run`
  - `mvn -pl client javafx:run`

## 2. Tai khoan test

Server tu tao san cac tai khoan:

- `admin / Admin@123`
- `admin1 / Admin@123`
- `admin2 / Admin@456`
- `bidder / 123456`
- `bidder1 / 123456`

Tai khoan seller khong co san, can dang ky moi trong app:

- `seller_a`
- `seller_b` (de test cheo quyen va du phong)

## 3. Du lieu nen tao de test

Dung `seller_a` tao 4 listing:

1. `ART-SCHEDULED-1`
   - Category: Art
   - Start date: hom nay
   - End date: hom nay + 1 ngay
   - Starting price: 100
   - Reserve price: 0
   - 2-3 anh

2. `WATCH-LIVE-1`
   - Category: Watches
   - Starting price: 100
   - Reserve price: 150
   - Dung de test bid, auto-bid, outbid, anti-sniping

3. `FASHION-NO-WINNER-1`
   - Category: Fashion
   - Starting price: 100
   - Reserve price: 1000
   - Dung de test case dau gia ket thuc nhung khong du reserve

4. `JEWELLERY-DELETE-1`
   - Category: Jewellery
   - Starting price: 200
   - Reserve price: 250
   - Dung de test edit/delete truoc khi admin tao auction

## 4. Flow 1 - Auth va phan quyen

### 4.1 Dang ky

1. Mo app, vao tab Register.
2. Test tung case:
   - username rong
   - email rong
   - email sai dinh dang
   - password rong
   - password < 6 ky tu
   - confirm password khong khop
   - role chua chon
3. Dang ky thanh cong `seller_a`.
4. Dang ky thanh cong `seller_b`.
5. Dang ky thanh cong 1 bidder moi neu can.

Ky vong:

- Moi case sai hien dung thong bao loi.
- Register thanh cong quay ve form login.
- Seller khong co trong role dang ky thi khong dang ky duoc Admin.

### 4.2 Dang nhap

1. Thu login voi username/email rong.
2. Thu login voi password rong.
3. Thu login voi password ngan hon 6 ky tu.
4. Thu login sai thong tin.
5. Login `bidder`.
6. Logout.
7. Login `seller_a`.
8. Logout.
9. Login `admin`.

Ky vong:

- `bidder` vao Showroom.
- `seller` vao Seller Dashboard.
- `admin` vao Admin Dashboard.
- User khong vao duoc scene sai role.

## 5. Flow 2 - Seller tao, sua, xoa listing

Dang nhap `seller_a`.

1. Vao tab overview, listings, inventory de check navigation.
2. Bam create listing.
3. Test validation:
   - product name rong
   - description rong
   - category rong
   - starting price rong / khong phai so / <= 0
   - reserve price < 0
   - reserve price < starting price
   - quantity rong / am / khong phai so nguyen
   - start date rong
   - end date rong
   - end date < start date
4. Chon 2-3 anh, doi primary image.
5. Tao `JEWELLERY-DELETE-1`.
6. Tao tiep `ART-SCHEDULED-1`, `WATCH-LIVE-1`, `FASHION-NO-WINNER-1`.
7. Search theo ten, category, status.
8. Sua `JEWELLERY-DELETE-1`, doi title, reserve, quantity, primary image.
9. Xoa `JEWELLERY-DELETE-1`.
10. Refresh dashboard.
11. Mo wallet tu seller dashboard.

Ky vong:

- Listing moi vao trang thai `PENDING`.
- Grid, recent listings, table, stats seller cap nhat.
- Da anh hien preview dung, primary image di len dau.
- Xoa duoc listing chua co auction.

## 6. Flow 3 - Admin review listing va tao auction

Dang nhap `admin`.

1. Vao Overview, check stats va wallet activity co load.
2. Vao Auction Management.
3. Review tung pending item vua tao.
4. Reject tam 1 item, kiem tra item bien mat khoi pending.
5. Dung item khac, bam Accept de approve va create auction.
6. Test `Delete` voi pending item.
7. Voi listing can dau gia, tao auction tu pending item.
8. Review lai auction vua tao.
9. Test cac action tren auction:
   - Accept auction
   - Reject auction
   - Delete auction
   - Purge auction

Ky vong:

- Pending item duoc approve tao ra auction.
- Reject item thi seller thay status reject/pending phu hop sau refresh.
- Auction management list hien dung state: scheduled, active, closed, rejected, deleted.

## 7. Flow 4 - Bidder browse, search, favorite, xem chi tiet

Dang nhap `bidder`.

1. Vao Showroom.
2. Test 4 filter:
   - All
   - Ending Soon
   - New Listings
   - My Wishlist
3. Search theo ten auction ton tai.
4. Search keyword khong ton tai.
5. Toggle favorite tren card showroom.
6. Vao Trending:
   - Hot
   - Most Viewed
   - Most Saved
   - Load more
   - Search co ket qua / khong co ket qua
7. Vao Category:
   - Art / Jewellery / Watches / Fashion
   - Search trong category
   - Save Category
   - Load More
8. Mo Product Detail tu 1 auction.
9. Kiem tra:
   - gallery anh
   - thumbnail doi main image
   - thong tin lot/category/description/specs
   - reserve status
   - countdown
   - winner notice card an khi auction chua dong
10. Search tu Product Detail va quay lai showroom.

Ky vong:

- Empty state hien dung khi search khong co du lieu.
- Favorite tren showroom/trending/category/won auctions tac dong len wishlist count.
- Product detail auto refresh du lieu auction.

## 8. Flow 5 - Wallet va rang buoc so du

Dang nhap `bidder`.

1. Mo Wallet.
2. Test nap tien:
   - rong
   - 0
   - am
   - chuoi khong hop le
   - nap thanh cong 10000
3. Test rut tien:
   - rut > balance
   - rut hop le
4. Quay lai Product Detail / Showroom, check balance cap nhat.

Ky vong:

- Deposit/withdraw hien success state.
- Balance cap nhat dung.
- Khong rut duoc vuot qua `availableToWithdraw`.

## 9. Flow 6 - Manual bid, bid dialog, live bidding, anti-sniping

Can 2 tai khoan: `bidder` va `bidder1`. Ca hai nap du tien truoc.

1. Dang nhap `bidder`, mo `WATCH-LIVE-1`.
2. Test place bid tren Product Detail:
   - chua nhap so tien
   - nhap ky tu khong phai so
   - nhap < min next bid
3. Mo Bid Dialog:
   - khong tick confirm
   - nhap bid < minimum
   - nhap bid hop le
4. Vao Live Bidding.
5. Mo client thu 2 bang `bidder1`, vao cung auction.
6. `bidder1` dat gia cao hon.
7. Quay lai client 1, kiem tra:
   - current bid doi
   - leader doi
   - lich su bid them dong moi
   - chart them diem
   - thong bao outbid
   - toast bid moi
8. Dat bid sat gio ket thuc de test anti-sniping.

Ky vong:

- Ca Product Detail va Live Bidding deu chot rule min-next-bid.
- Realtime cap nhat ca 2 client.
- Khi bid trong nguong cuoi, end time duoc gia han them.

## 10. Flow 7 - Auto-bid day du

Can 2 tai khoan: `bidder` va `bidder1`.

1. `bidder` mo Product Detail hoac Live Bidding cua `WATCH-LIVE-1`.
2. Mo Auto-Bid dialog.
3. Test:
   - max bid rong
   - max bid < minimum
   - bid step < min increment
   - khong tick confirm
4. Bat auto-bid: max = 500, step = 20.
5. Dang nhap `bidder1`, dat manual bid 150, 170, 190...
6. Quan sat `bidder` tu dong phan hoi tang gia den muc max.
7. Bat lai auto-bid voi max moi de test replace.
8. Tat auto-bid.
9. Thu tat auto-bid khi khong co auto-bid active.

Ky vong:

- Auto-bid chi cho phep tren auction `ACTIVE` hoac `SCHEDULED`.
- Seller khong duoc bat auto-bid tren auction cua minh.
- Replace auto-bid thanh cong.
- Tat auto-bid thanh cong va state ve inactive.

## 11. Flow 8 - Ket thuc auction, reserve, winner, won auctions

### 8.1 Case co winner

1. Dam bao `WATCH-LIVE-1` ket thuc voi gia >= reserve.
2. Sau khi dong auction, dang nhap tai khoan thang.
3. Mo Product Detail cua auction do.
4. Kiem tra:
   - bid controls bi khoa
   - winner notice hien "You won this auction"
   - vi bi tru tien
   - button mo Won Auctions hien
5. Vao Won Auctions.
6. Kiem tra summary:
   - total wins
   - total spent
   - latest win
   - notification card
   - card "View Win Detail"

### 8.2 Case khong du reserve

1. Dam bao `FASHION-NO-WINNER-1` ket thuc voi gia < reserve.
2. Mo Product Detail sau khi dong.

Ky vong:

- Hien "Auction finished with no winner".
- Khong co winner notice success.
- Khong tru tien bidder.
- Auction khong xuat hien trong Won Auctions.

## 12. Flow 9 - Chot quyen sua/xoa sau khi auction da duoc tao

Dang nhap lai `seller_a`.

1. Mo listing da duoc admin tao auction nhung chua start.
2. Thu edit listing.
3. Thu delete listing.
4. Khi auction van con `scheduled`, kiem tra co cho sua/xoa hay khong theo start time thuc te.
5. Sau khi auction da live:
   - thu edit
   - thu delete

Ky vong:

- Listing chi sua/xoa duoc truoc gio start.
- Khi auction da live: thong bao chan dung ly do.
- Sau khi seller sua listing da linked auction, item quay lai `PENDING` de admin duyet lai.

## 13. Flow 10 - API sanity check cho chuc nang co trong server nhung UI chua lo het

Nen test bang Postman hoac `Invoke-RestMethod` voi token dang nhap:

- `GET /api/wallet/history`
- `GET /api/auctions/{id}/summary`
- `GET /api/auctions/{id}/leader`
- `GET /api/auctions/{id}/auto-bids`
- `GET /api/auctions/me/auto-bids`
- `GET /api/auctions/me/wins`
- `POST /api/auctions/{id}/close`
- `GET /admin/notifications/recent`

Ky vong:

- Response schema hop ly, khong 500.
- Phan quyen dung role.
- Du lieu dong bo voi UI.

## 14. Checklist micro-case de khong bo sot

- Home:
  - search tu home sang showroom
  - cac nut category/trending/auth
- Auth:
  - login bang username
  - login bang email
  - logout o moi role
- Showroom/Trending/Category:
  - empty state
  - search state duoc reset dung
  - wishlist count tang/giam
- Product Detail:
  - dau gia `SCHEDULED`: nut bid bi khoa, auto-bid van mo
  - dau gia `CLOSED`: ca bid va auto-bid bi khoa
  - reserve met / reserve not met
  - detail tu dong refresh khi auction dong
- Live Bidding:
  - socket reconnect/polling state label
  - bid history dialog mo duoc
  - auto-bid dialog mo duoc
- Seller:
  - multi-image upload
  - primary image swap
  - recent listings cap nhat
  - search theo status/category/title
- Admin:
  - review panel enable/disable button dung theo selection
  - wallet activity co load
  - stats fallback van hien khi API stats loi
- Wallet:
  - back button tra dung man theo role
  - rut tien khi dang co tien bi reserve cho active bids

## 15. Thu tu chay de it ton cong nhat

1. Flow 1
2. Flow 2
3. Flow 3
4. Flow 4
5. Flow 5
6. Flow 6
7. Flow 7
8. Flow 8
9. Flow 9
10. Flow 10

Neu ban muon day nay thanh test case co cot `ID / Step / Expected / Pass-Fail / Evidence`, can tach tu file nay ra thanh bang QC chuan de team test dung truc tiep.
