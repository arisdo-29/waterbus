# DEMO_GUIDE.md - Hướng dẫn chạy và demo Saigon Waterbus

## 1. Chạy project

1. Tạo database: mở SSMS, chạy `database/WaterbusDB_SQLServer.sql` (tạo `WaterbusDB` + dữ liệu mẫu).
   **Chạy lại script ngay trước buổi demo**: chuyến mẫu chỉ có 7 ngày kể từ ngày chạy script.
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
| 5 | Nhập họ tên, SĐT, email, mã KM → "Đặt vé" | `/booking` (POST) rồi chuyển `/payment/{mã}` | `BookingController.create` | `booking` (PENDING), `ticket` (HOLD) |
| 6 | Bấm "Thanh toán" (mô phỏng) | `/payment/{mã}/mock` (POST) rồi `/booking/{mã}` | `PaymentController.payMock` | `payment` (SUCCESS), `booking` (PAID), `ticket` (VALID), `promotion.used_count` |
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

**Thanh toán mô phỏng.** `PaymentService.payMock` (một transaction) chỉ nhận đơn PENDING còn hạn, rồi tạo `payment`
SUCCESS, đổi `booking` sang PAID và `ticket` từ HOLD sang VALID. Không có cổng thanh toán thật.

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
