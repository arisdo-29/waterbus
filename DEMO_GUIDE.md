# DEMO_GUIDE.md - Hướng dẫn chạy và demo Saigon Waterbus

## 1. Chạy project

1. Tạo database: mở SSMS, chạy `database/WaterbusDB_SQLServer.sql` (tạo `WaterbusDB` + dữ liệu mẫu).
   **Chạy lại script ngay trước buổi demo**: chuyến mẫu chỉ có 7 ngày kể từ ngày chạy script.
   Script KH3 đã gộp schema/seed của ba migration cũ và tự xoá bảng phụ theo đúng thứ tự khoá ngoại;
   máy mới không cần chạy thêm các migration lịch sử. DB đang dùng chỉ cần nâng cấp CASH/tên mã thì chạy
   `database/changes/2026-11-13_db_tien-mat-ten-khuyen-mai.sql`, không xoá-tạo DB cần giữ dữ liệu.
   Lịch phân công tàu còn chờ TI7; chưa dùng nhánh KH3 làm dữ liệu demo chính thức trước khi hết chồng giờ.
2. Cấu hình mật khẩu SQL Server: copy `src/main/resources/application-local.properties.example`
   thành `application-local.properties`, sửa `spring.datasource.password` (file này bị gitignore). Chi tiết ở `docs/SETUP.md`.
3. Chạy: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`), mở http://localhost:8080.
4. Kiểm tra trước khi commit: `./mvnw -q test` (dùng H2 trong RAM, không cần SQL Server).

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Quản trị | `admin@waterbus.vn` | `Admin@123` |
| Nhân viên (soát vé) | `staff@waterbus.vn` | `Staff@123` |
| Khách | `customer@gmail.com` | `Customer@123` |

Mã khuyến mãi mẫu: `WELCOME10` (giảm 10%, tối đa 10.000 đ), `GIAM5K` (giảm 5.000 đ cho đơn từ 30.000 đ).

## 2. Kịch bản demo: đặt vé

| Bước | Bấm gì | Trang | Controller xử lý | Dữ liệu ghi vào bảng |
|---|---|---|---|---|
| 1 | Mở trang chủ | `/` | `HomeController.home` | chỉ đọc `station` |
| 2 | Chọn nơi đi, nơi đến, ngày → "Tìm chuyến" | `/trips?...` | `TripController.index` | chỉ đọc `trip`, `route`, `seat`, `ticket` |
| 3 | Bấm vào một chuyến | `/trips/{id}/seats` | `TripController.seats` | chỉ đọc |
| 4 | Chọn ghế + loại vé → "Tiếp tục" | `/booking/checkout` (POST) | `BookingController.checkout` | chưa ghi gì |
| 5 | Nhập liên hệ, thông tin hành khách nếu là vé ưu đãi, mã KM và đồng ý chính sách → "Đặt vé" | `/booking` (POST); tổng > 0 → `/payment/{mã}`, tổng = 0 → `/booking/{mã}` | `BookingController.create` | Tổng > 0: `booking` PENDING, `ticket` HOLD; tổng = 0: PAID, VALID |
| 6 | Bấm thanh toán VNPay, trả bằng thẻ test trên Sandbox rồi quay về | POST `/payment/{mã}/vnpay` → VNPay → GET `/payment/vnpay-return` → `/booking/{mã}` khi thành công | `PaymentController.payVnPay`, `vnPayReturn` | `payment` SUCCESS, `booking` PAID, `ticket` VALID, tăng `promotion.used_count` |
| 7 | Xem vé + mã QR | `/booking/{mã}` | `BookingController.detail` | chỉ đọc |
| 8 | Menu tên người dùng → "Vé của tôi" | `/my-bookings` | `MyBookingController.history` | chỉ đọc |

Kịch bản phụ: đặt xong **không** thanh toán, bấm "Huỷ đơn" ở `/booking/{mã}` (`BookingController.cancel`),
hoặc chờ 10 phút để đơn tự hết hạn. Cả hai đều trả ghế về trống.
Thử lỗi: bỏ trống họ tên, nhập SĐT sai, nhập mã KM sai → lỗi hiện dưới từng ô.

## 3. Kịch bản demo: tài khoản và admin

| Việc | URL | Controller | Bảng |
|---|---|---|---|
| Đăng ký | `/auth/register` | `AuthController.register` | `account` (mật khẩu BCrypt) |
| Đăng nhập / đăng xuất | `/auth/login`, `/auth/logout` | `AuthController` | session `loginAccount` |
| Sửa hồ sơ, đổi mật khẩu | `/account/profile`, `/account/change-password` | `AccountController` | `account` |
| Quản lý chuyến, tàu | `/admin/trips`, `/admin/waterbuses` | `AdminTripController`, `WaterbusController` | `trip`, `waterbus`, `seat` |
| Quản lý bến, tuyến | `/admin/stations`, `/admin/routes` | `AdminStationController`, `AdminRouteController` | `station`, `route`, `route_station` |
| Quản lý khuyến mãi, loại vé | `/admin/promotions`, `/admin/ticket-types` | `PromotionController`, `TicketTypeController` | `promotion`, `ticket_type` |
| Soát vé | `/admin/checkin` | `AdminCheckinController` | `ticket` (VALID → USED) |

Đăng nhập bằng khách rồi vào `/admin/trips` sẽ bị chuyển về trang chủ (`AdminInterceptor`).

## 4. Giải thích ngắn từng chức năng (để trả lời giảng viên)

**Tìm chuyến và chọn ghế.** Form trang chủ gửi GET `/trips` với bến đi, bến đến, ngày. `TripController` gọi
`TripService.searchTripResults` để lấy các chuyến có chạy qua hai bến theo đúng thứ tự, kèm số ghế còn trống.
Trang chọn ghế lấy sơ đồ ghế từ `TripService.getSeatMap`; ghế đã có vé HOLD hoặc VALID hiện là "Đã bán" và không chọn được.

**Checkout (xem lại đơn).** `BookingController.checkout` kiểm tra đã chọn ghế và ghế còn trống, rồi tính tạm tính
(giá loại vé × số ghế). Bước này chưa lưu gì vào database; dữ liệu đi tiếp qua các ô ẩn (`tripId`, `seatIds`...) trong form.

**Tạo đơn.** `BookingController.create` kiểm tra form bằng `@Valid` (`BookingForm`), rồi `BookingService.createBooking`
kiểm tra lại ghế, bến, loại vé, mã khuyến mãi và lưu `booking` (PENDING, hạn 10 phút) cùng mỗi ghế một `ticket` (HOLD),
trong một `@Transactional`. Lỗi thì hiện lại trang checkout, thành công thì `redirect` sang thanh toán (bấm F5 không tạo đơn thứ hai).

**Khuyến mãi.** `PromotionService.calculateDiscount` kiểm tra mã còn hạn, còn lượt, đủ điều kiện tối thiểu rồi tính giảm
(PERCENT có trần `max_discount`, FIXED giảm số tiền cố định). Lượt dùng chỉ tăng khi thanh toán thành công.

**Thanh toán VNPay.** `VnPayService` tạo URL sang Sandbox; khi cổng trả khách về, backend kiểm tra chữ ký,
số tiền và hiệu lực đơn trước khi gọi `PaymentService.confirmPaid`. Thanh toán thành công tạo `payment`
SUCCESS, đổi `booking` sang PAID và `ticket` từ HOLD sang VALID. Sandbox dùng tiền thử nghiệm.

**Đơn tổng 0 đ.** `BookingService.createBooking` xác nhận PAID và vé VALID ngay, không tạo giao dịch thanh toán.
Có thể dùng mã `FREE100` để demo luồng này; mã có giới hạn lượt nên phải kiểm tra dữ liệu trước buổi demo.

**Giữ chỗ và hết hạn.** `BookingExpiryJob` chạy mỗi 60 giây, đổi đơn PENDING quá hạn sang EXPIRED và vé HOLD sang CANCELLED
để ghế được bán lại. Database có unique index `ux_ticket_trip_seat_active` để hai người không thể giữ cùng một ghế.

**Đăng nhập và phân quyền.** `AuthController.login` kiểm tra mật khẩu bằng BCrypt, lưu `AccountDTO` vào session với tên
`loginAccount`. Navbar đọc session để hiện tên. `AdminInterceptor` chặn mọi URL `/admin/**` nếu không phải ADMIN hoặc STAFF.
Đặt vé khi chưa đăng nhập vẫn được (khách vãng lai, `account_id` để NULL).

**Giao diện dùng chung.** Mọi trang gọi các fragment trong `templates/fragments/layout.html` (head, navbar, alerts, footer, scripts)
nên header, footer và thông báo thành công/lỗi giống nhau. Controller chỉ cần `flash.addFlashAttribute("successMessage", ...)`
hoặc `errorMessage` là fragment `alerts` tự hiện.

## 5. Thư viện giao diện đang dùng

| Thư viện | Lý do |
|---|---|
| Bootstrap 5.3.3 (file local trong `static/`) | Lưới, form, nút, card, navbar, bảng và responsive; chạy được khi mất mạng |
| Bootstrap Icons 1.11.3 (file local trong `static/css`) | Bộ icon đồng bộ với Bootstrap, đã tải về để demo không cần Internet |
| Thymeleaf fragment (`th:fragment`, `th:replace`) | Tách navbar, footer, thông báo dùng chung mà không cần thêm thư viện |
| Google Fonts "Be Vietnam Pro" | Font hỗ trợ tiếng Việt đẹp; mất mạng thì tự dùng font hệ thống |
| `qrcode.min.js` | Vẽ mã QR cho từng vé ở trang `/booking/{mã}` |

Không dùng: Thymeleaf Layout Dialect (fragment đã đủ), SweetAlert2 (alert Bootstrap đã đủ), Flatpickr (ô `type="date"` đã đủ).

## 6. Thanh toán VNPay Sandbox

Trang `/payment/{mã đơn}` hiện thanh toán qua **VNPay**: form gửi `POST /payment/{mã đơn}/vnpay`,
BE4 chuyển hướng sang cổng VNPay, cổng trả về `GET /payment/vnpay-return`.
Endpoint thanh toán mô phỏng `/payment/{mã đơn}/mock` đã bỏ. Nhãn MOCK trên đơn mẫu cũ chỉ dùng để đọc lịch sử.

1. Đăng ký tài khoản test tại https://sandbox.vnpayment.vn/devreg/ để nhận `TmnCode` và `HashSecret` qua email.
2. Ghi vào `src/main/resources/application-local.properties` (file bị gitignore, **không commit**).
   Chỉ cần điền 2 khoá:
   ```properties
   vnpay.tmn-code=MA_TMN_CUA_BAN
   vnpay.hash-secret=CHUOI_BI_MAT_CUA_BAN
   ```
   `vnpay.pay-url` và `vnpay.return-url` đã có giá trị mặc định trong `VnPayConfig`
   (`https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` và `http://localhost:8080/payment/vnpay-return`);
   chỉ khai báo lại khi chạy cổng khác 8080 hoặc dùng cổng VNPay khác.
   Thiếu `tmn-code` hoặc `hash-secret` thì app vẫn chạy, nút VNPay báo "VNPay chưa được cấu hình trên máy chủ".
3. Thẻ test (ngân hàng NCB) theo tài liệu VNPay Sandbox - **kiểm tra lại trên sandbox.vnpayment.vn nếu đổi**:

   | Trường | Giá trị |
   |---|---|
   | Ngân hàng | NCB |
   | Số thẻ | 9704198526191432198 |
   | Tên chủ thẻ | NGUYEN VAN A |
   | Ngày phát hành | 07/15 |
   | Mật khẩu OTP | 123456 |

4. Thành công → về `/booking/{mã đơn}` kèm thông báo xanh, trang vé hiện "Phương thức: VNPay" và mã giao dịch.
   Thất bại/huỷ → về `/payment/{mã đơn}` kèm thông báo đỏ, vẫn chọn lại được nếu còn hạn giữ chỗ.
5. Chọn sẵn phương thức: nút VNPay gửi kèm `bankCode` trên URL (`VNPAYQR` mở thẳng màn hình mã QR,
   `VNBANK` mở thẳng trang nhập thẻ ATM nội địa; không có `bankCode` thì VNPay hiện trang cho khách tự chọn).
   **Mã QR trên Sandbox chỉ để trình chiếu, không quét bằng app ngân hàng thật.**
   Khi demo: chọn QR để mở màn hình mã QR cho giảng viên xem → bấm **Huỷ** trên VNPay (về trang thanh toán, báo đỏ)
   → chọn lại **thẻ ATM** và trả bằng thẻ NCB ở bảng trên.

Buổi demo không có mạng hoặc chưa cấu hình VNPay: có thể trình bày luồng đơn 0 đ bằng `FREE100` nếu còn lượt;
không coi đây là kiểm chứng thanh toán qua cổng. Muốn demo đơn có tiền phải chuẩn bị VNPay và mạng trước.

## 7. Thay đổi đang chờ tích hợp sau rà soát 12/11/2026

Theo [phân công KH1–KH5 và TR/TI](https://app.notion.com/p/416be562b59581cc8f83c7b2051e048b):
form SĐT xác minh đơn chờ TR2; đăng nhập/giới hạn vé ưu đãi chờ TR3; bỏ mốc 13 tuổi chờ TR4;
nhãn vé đã qua chuyến chờ TI1/TR5; thu tiền mặt chờ KH3/TR6. Chỉ đưa các bước này vào kịch bản chính sau khi tích hợp.
