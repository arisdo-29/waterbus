# AUDIT.md - Rà soát source code Saigon Waterbus

Ngày rà soát: 26/10/2026 · Nhánh `develop` · Giai đoạn 1 (chỉ đọc, chưa sửa code).

**Cách rà soát:** đọc toàn bộ controller, `BookingServiceImpl`, `PaymentServiceImpl`, `TripServiceImpl`,
`layout.html` và các template của luồng đặt vé, `application.properties`, `pom.xml`, `docs/PAGES.md`.
Chạy `./mvnw -q test`: **tất cả test pass (exit 0)**. Chưa chạy app với SQL Server và chưa xem giao diện
bằng trình duyệt, nên các mục giao diện bên dưới là suy ra từ code, cần xác nhận khi chạy thật.

---

## 1. Frontend dùng công nghệ gì

**Thymeleaf (server-side rendering), không phải React.** Không có `package.json`, không có thư mục React.
Bản bolt.new ban đầu đã được chuyển thành template Thymeleaf.

| Thành phần | Hiện trạng |
|---|---|
| Template | `src/main/resources/templates/**` (34 file HTML), Thymeleaf |
| Layout chung | `fragments/layout.html` dùng `th:fragment`: `head(title)`, `navbar`, `alerts`, `footer`, `scripts`, `statusBadge(status)`, `adminSidebar`. **Đã có**, không cần Thymeleaf Layout Dialect |
| Bootstrap 5.3.3 | **Đã có**, file CSS/JS nằm sẵn trong `static/css`, `static/js` (chạy offline). Trong `<head>` có thêm link CDN nhưng bị `th:href` thay bằng bản local |
| Bootstrap Icons 1.11.3 | **Đã có, nhưng nạp từ CDN** (jsdelivr) |
| Font Be Vietnam Pro | **Nạp từ Google Fonts (CDN)** |
| CSS riêng | `app.css` (37 KB, biến màu tông xanh, class `wb-*`), `admin.css` (11 KB) |
| JS riêng | `app.js` (25 KB: chọn ghế, đếm ngược giữ chỗ, đổi chiều bến, validate form tìm chuyến), `qrcode.min.js` (QR cho vé) |
| Inline `style="..."` | Không có (tốt) |

Kết luận: **Giai đoạn 3 đã làm được phần lớn** (Bootstrap, icon, navbar/footer chung, stepper 4 bước, alert,
badge trạng thái, sidebar admin). Việc còn lại là dọn và đồng bộ, không phải làm mới.

## 2. Cấu trúc backend

Package gốc `com.hsf302.waterbus`, kiến trúc **Controller → Service (interface) → ServiceImpl → Repository → Entity**, có DTO.
Không dùng Lombok, inject bằng constructor, 12 repository, ~5.900 dòng Java.

| Lớp | Danh sách |
|---|---|
| Controller (16) | `HomeController`, `AuthController`, `AccountController`, `StationController`, `RouteController`, `TripController`, `BookingController`, `PaymentController`, `MyBookingController`, `AdminStationController`, `AdminRouteController`, `AdminTripController`, `WaterbusController`, `PromotionController`, `TicketTypeController`, `AdminCheckinController` |
| Service (10, +impl) | `Account`, `Booking`, `Payment`, `Promotion`, `Route`, `Station`, `Ticket`, `TicketType`, `Trip`, `Waterbus` |
| Repository (12) | `Account`, `Booking`, `Payment`, `Promotion`, `Route`, `RouteStation`, `Seat`, `Station`, `Ticket`, `TicketType`, `Trip`, `Waterbus` |
| Entity (12) | `Account`, `Booking`, `Payment`, `Promotion`, `Route`, `RouteStation`, `Seat`, `Station`, `Ticket`, `TicketType`, `Trip`, `Waterbus` |
| Khác | `config/WebMvcConfig` (đăng ký interceptor), `interceptor/AdminInterceptor` (chặn `/admin/**` nếu không phải ADMIN/STAFF), `job/BookingExpiryJob` (mỗi 60 giây huỷ đơn PENDING quá hạn 10 phút) |

Phân quyền: không dùng Spring Security, chỉ session `loginAccount` (AccountDTO) + `AdminInterceptor`. Mật khẩu BCrypt.
Test: 21 file (H2 trong RAM) + 2 script browser trong `src/test/js`.

## 3. Bảng luồng chuyển trang

### 3.1 Luồng chính: đặt vé

| # | URL | Controller method | Template | Đi tiếp bằng |
|---|---|---|---|---|
| 1 | `GET /` | `HomeController.home` | `index` | Form tìm chuyến (nơi đi, nơi đến, ngày) `GET /trips` |
| 2 | `GET /trips?fromStationId&toStationId&date` | `TripController.index` | `trip/index` | Mỗi chuyến có nút/link sang `/trips/{id}/seats?fromStationId&toStationId` |
| 3 | `GET /trips/{id}/seats` | `TripController.seats` (chuyến không tồn tại → `redirect:/trips`) | `trip/seats` | Form chọn ghế + loại vé, `POST /booking/checkout` |
| 4 | `POST /booking/checkout` | `BookingController.checkout` (không chọn ghế / ghế bị đặt → `redirect` về trang 3 kèm `errorMessage`) | `booking/checkout` | Form thông tin liên hệ + mã KM, `POST /booking`; nút "Quay lại" dùng `history.back()` |
| 5 | `POST /booking` | `BookingController.create` (lỗi → render lại `booking/checkout`; thành công → redirect) | `booking/checkout` hoặc `redirect:/payment/{code}` | Tạo `booking` (PENDING) + `ticket` (HOLD), giữ ghế 10 phút |
| 6 | `GET /payment/{code}` | `PaymentController.index` (sai mã → `redirect:/` + lỗi) | `payment/index` | Nút "Thanh toán" `POST /payment/{code}/mock`; JS đếm ngược hạn giữ chỗ |
| 7 | `POST /payment/{code}/mock` | `PaymentController.payMock` | `redirect:/booking/{code}` (lỗi → `redirect:/payment/{code}`) | Ghi `payment` SUCCESS, `booking` PAID, `ticket` VALID, tăng `used_count` mã KM |
| 8 | `GET /booking/{code}` | `BookingController.detail` | `booking/detail` | Xem vé + QR; nút "Huỷ đơn" `POST /booking/{code}/cancel` khi PENDING |
| 9 | `GET /my-bookings` | `MyBookingController.history` (chưa đăng nhập → `/auth/login`) | `booking/history` | Mỗi đơn có link `/booking/{code}` |

Luồng này **liền mạch về mặt code**: mỗi bước có đích đi tiếp, lỗi đều quay về trang hợp lý và hiện `errorMessage`.

### 3.2 Đăng nhập / tài khoản

| URL | Controller method | Template | Ghi chú |
|---|---|---|---|
| `GET /auth/login` | `AuthController.showLoginForm` | `auth/login` | Đã đăng nhập → `redirect:/` |
| `POST /auth/login` | `AuthController.login` | `auth/login` / `redirect:/` | Lưu `loginAccount` vào session |
| `GET /auth/register` | `showRegisterForm` | `auth/register` | |
| `POST /auth/register` | `register` | `redirect:/auth/login` + `successMessage` | |
| `POST /auth/logout` (và `GET`) | `logout` / `logoutGet` | `redirect:/` | |
| `GET/POST /account/profile` | `AccountController` | `account/profile` | Chưa đăng nhập → `/auth/login` |
| `GET/POST /account/change-password` | `AccountController` | `account/change-password` | Xong → `redirect:/account/profile` |

**Lưu ý:** sau khi đăng nhập luôn về `/`, không quay lại trang đang làm dở. Navbar chưa có link tới `/account/profile`
(dropdown chỉ có "Vé của tôi", "Quản trị", "Đăng xuất").

### 3.3 Trang công khai khác

`GET /stations` → `station/list` → `GET /stations/{id}` → `station/view`; `GET /routes` → `route/index`.

### 3.4 Trang admin (qua `AdminInterceptor`, cần ADMIN/STAFF)

| URL | Controller | Template |
|---|---|---|
| `/admin/trips`, `/new`, `POST /save`, `POST /{id}/cancel` | `AdminTripController` | `admin/trip/list`, `form` |
| `/admin/waterbuses`, `/new`, `/{id}/edit`, `POST /save`, `POST /{id}/status` | `WaterbusController` | `admin/waterbus/list`, `form` |
| `/admin/stations`, `/new`, `/{id}/edit`, `POST /save`, `POST /{id}/delete` | `AdminStationController` | `admin/station/list`, `form` |
| `/admin/routes` (cùng mẫu như stations) | `AdminRouteController` | `admin/route/list`, `form` |
| `/admin/promotions`, `/create`, `/{id}/edit`, `POST /save`, `POST /{id}/delete` | `PromotionController` | `admin/promotion/list`, `form` |
| `/admin/ticket-types` (cùng mẫu) | `TicketTypeController` | `admin/tickettype/list`, `form` |
| `GET/POST /admin/checkin` | `AdminCheckinController` | `admin/checkin/index` |

Tài khoản seed trong `database/WaterbusDB_SQLServer.sql`: `admin@waterbus.vn` (ADMIN), `staff@waterbus.vn` (STAFF),
`customer@gmail.com` (CUSTOMER). Mật khẩu chỉ có dạng hash BCrypt trong script, cần xem `docs/SETUP.md` để ghi vào DEMO_GUIDE.
Mã KM seed: `WELCOME10`, `GIAM5K`.

## 4. Danh sách vấn đề

### (A) Có thể làm hỏng demo

Không tìm thấy lỗi logic chắc chắn trong luồng chính (test pass, các bước nối nhau đúng). Các rủi ro thật sự:

| # | Vấn đề | Vị trí | Hướng xử lý |
|---|---|---|---|
| A1 | **Phụ thuộc Internet khi demo**: Bootstrap Icons và Google Fonts nạp từ CDN. Mất mạng ở phòng demo → icon vỡ thành ô trống, font đổi | `fragments/layout.html` (head) | Tải file icon/font về `static/` giống cách đã làm với Bootstrap, hoặc chấp nhận rủi ro. Không cần sửa pom |
| A2 | **Dữ liệu chuyến seed chỉ có 7 ngày kể từ lúc chạy script SQL**. Chạy script cách demo hơn 7 ngày thì tìm chuyến ra rỗng | `database/WaterbusDB_SQLServer.sql` (đoạn `;WITH days`) | Chạy lại script (hoặc script bổ sung chuyến) ngay trước buổi demo. Ghi vào DEMO_GUIDE |
| A3 | `POST /booking/checkout` và `POST /booking` với `tripId`/`ticketTypeId` không tồn tại (sửa tay URL/form): `getTripInfo`/`getTicketTypeById` trả `null`, trang checkout vẫn render với dữ liệu trống, không báo lỗi | `BookingController.fillCheckoutModel` | Nếu `trip` hoặc `ticketType` null thì redirect về `/trips` kèm `errorMessage`. Hiếm gặp khi demo bình thường |
| A4 | Chưa kiểm tra chạy thật với SQL Server (test dùng H2). Lệch kiểu/constraint giữa H2 và SQL Server chỉ lộ khi chạy thật, ví dụ index `ux_ticket_trip_seat_active` | toàn bộ | Chạy app thật, đi hết luồng 1 → 9 và ghi kết quả vào DEMO_GUIDE |

### (B) Code khó hiểu / trùng lặp / thừa

| # | Vấn đề | Vị trí | Hướng xử lý |
|---|---|---|---|
| B1 | **Trang placeholder còn sót**: "Trang mẫu của module…" | `templates/booking/index.html`, `templates/admin/waterbus/index.html`; `BookingController.index()` (`GET /booking`, còn `// TODO BE4`) | Đề xuất `GET /booking` redirect về `/my-bookings` hoặc xoá. **Đụng URL** nên cần bạn duyệt |
| B2 | Thư mục mẫu `templates/admin/_template/` không controller nào dùng | `templates/admin/_template/*` | Có thể xoá (chỉ là mẫu cho nhóm) |
| B3 | **Hack `EntityManager`** trong `BookingServiceImpl` chỉ để lấy `Trip`, có TODO chờ Trí thêm `TripService.getTripById` | `BookingServiceImpl.findTrip`, constructor | Thêm 1 method vào `TripService` (được phép thêm) rồi bỏ `EntityManager`. Liên quan module BE2 + BE4 |
| B4 | Comment `TODO: thay các dòng throw bằng code thật` đã lỗi thời (service đã làm xong) | `WaterbusServiceImpl` dòng 14 | Xoá comment |
| B5 | `TripServiceImpl`: import `Trip` **2 lần**, import lộn xộn, vài comment tiếng Anh, lọc route/tàu bằng `stream().filter` trên toàn bộ danh sách thay vì tìm theo id | `TripServiceImpl` | Gọn import, dịch comment. Phần lọc để nguyên, chỉ giải thích |
| B6 | Cùng 1 đoạn "lấy booking → đưa `booking/trip/tickets` vào model" lặp ở `BookingController.detail` và `PaymentController.index` | 2 controller | Chấp nhận (2 chỗ, dễ hiểu), chỉ cần comment. Không gộp |
| B7 | Quy tắc SĐT `0[0-9]{9}` lặp ở 3 nơi: HTML `pattern`, `BookingForm`, `BookingServiceImpl` | `checkout.html`, `BookingForm`, service | Giữ (validate nhiều lớp), thêm comment nói rõ |
| B8 | `AuthController`: `GET /auth/logout` bên cạnh `POST /auth/logout` | `AuthController.logoutGet` | Giữ nếu test/ giao diện cần; ghi chú lý do |
| B9 | Template còn **nhiều khối giả lập từ bolt.new**: `th:remove="all"` (vd 17 ghế mẫu trong `trip/seats.html`, option mẫu trong `index.html`), link tĩnh `/templates/...html` làm fallback, `th:*` và giá trị cứng cùng 1 thẻ. Nhiều section nằm trên **1 dòng rất dài** (`checkout.html`, `seats.html`, `index.html`) | `index.html`, `trip/*`, `booking/*`, `payment/*` | Xuống dòng/thụt lề để dễ đọc; xoá khối `th:remove="all"`. **Giữ logic và tên field**. Đây là file của Khuyên/Trí, cần hỏi bạn là ai |
| B10 | `PaymentService.java` đang **sửa dở chưa commit**: thêm 1 dòng comment khó hiểu ("kế thừa PaymentService… lỡ đổi thành momo") | `service/PaymentService.java` | Hỏi bạn: giữ lại, viết lại cho rõ, hay bỏ |
| B11 | `docs/PAGES.md` còn đoạn "Trạng thái tích hợp tại `49bca30`… BE4 chưa triển khai" đã sai | `docs/PAGES.md` | Cập nhật khi sửa |
| B12 | `run.log` (log chạy app) nằm ở thư mục gốc, chưa bị gitignore | gốc repo | Xoá file, thêm `*.log` vào `.gitignore` (file chung, cần bạn đồng ý) |
| B13 | `createBooking` dài (~120 dòng, 8 bước) | `BookingServiceImpl` | Đã chia bước bằng comment và đọc được. **Không tách**, chỉ giữ comment |

### (C) Giao diện chưa đẹp / chưa đồng bộ

| # | Vấn đề | Vị trí | Hướng xử lý |
|---|---|---|---|
| C1 | Trang checkout **hiện lỗi 2 lần**: fragment `alerts` đã in `errorMessage`, ngay trong form lại in thêm 1 alert nữa | `booking/checkout.html` | Bỏ 1 trong 2 |
| C2 | Checkout chưa hiện lỗi **dưới từng ô** (`th:errors`) dù `docs/PAGES.md` đã mô tả sẵn và controller đã có `bookingForm` | `booking/checkout.html` | Thêm `th:object` + `invalid-feedback` đúng như PAGES.md |
| C3 | `head` fragment luôn nạp cả `admin.css` cho trang khách | `fragments/layout.html` | Chỉ nạp ở trang admin (hoặc chấp nhận, 11 KB) |
| C4 | Các trang `booking/index`, `admin/waterbus/index` là placeholder xám, lệch phong cách | xem B1 | Xoá |
| C5 | Dropdown tài khoản thiếu link "Thông tin cá nhân" (`/account/profile`); sidebar admin thiếu "Soát vé" (`/admin/checkin`) | `fragments/layout.html` | Thêm 2 link |
| C6 | Stepper 4 bước có ở seats/checkout/payment; cần xác nhận bước 1 (`trip/index`) và trang `booking/detail` (xong) hiển thị đúng | `trip/index.html`, `booking/detail.html` | Kiểm tra khi chạy thật |
| C7 | Mức đồng bộ màu/font/khoảng cách giữa trang khách, auth, admin: chưa xem bằng mắt | toàn bộ | Chụp từng trang khi chạy thật rồi mới chốt danh sách sửa |
| C8 | `login.html` có `<head>` fallback khác các trang khác (không ảnh hưởng chạy) | `auth/*.html` | Bỏ qua |

## 5. Đề xuất thư viện UI

Dự án đã có phần lớn, nên đề xuất **rất ít thay đổi**:

| Thư viện | Đề xuất | Lý do |
|---|---|---|
| Bootstrap 5.3.3 | **Giữ** (đã local) | Đã dùng khắp nơi |
| Bootstrap Icons 1.11.3 | **Giữ, chuyển sang file local** trong `static/` (không dùng WebJars) | Hết phụ thuộc mạng; copy file CSS + font vào `static/`, không động `pom.xml` (file chung), dễ giải thích hơn WebJars |
| Thymeleaf Layout Dialect | **Không thêm** | `th:fragment` / `th:replace` đã đủ; thêm dialect = thêm dependency, thêm khái niệm cho người mới |
| WebJars | **Không dùng** | Cần sửa `pom.xml`, và bản local hiện tại đã chạy |
| Font Be Vietnam Pro | Giữ qua Google Fonts, hoặc tải `woff2` về `static/fonts` | Nếu mất mạng, trình duyệt tự dùng font hệ thống, không vỡ layout |
| SweetAlert2 | **Không cần** | Alert Bootstrap (`successMessage`/`errorMessage`) đã đủ rõ |
| Flatpickr | **Không cần** | `<input type="date">` hoạt động trên mọi trình duyệt hiện đại |

## 6. Kế hoạch đề xuất cho Giai đoạn 2–3 (chờ bạn duyệt)

Mỗi mục là 1 commit nhỏ, message tiếng Anh, không tên agent, test pass trước mỗi commit:

1. Dọn comment/import: B4, B5 (không đổi hành vi).
2. Dọn file thừa: B2, B12, B4 + cập nhật `PAGES.md` (B11).
3. Checkout: C1 (bỏ lỗi trùng), C2 (lỗi dưới từng ô), A3 (redirect khi trip/ticketType null).
4. B1: xử lý `GET /booking` và hai trang placeholder (cần bạn duyệt vì đụng URL).
5. B3: bỏ `EntityManager` bằng `TripService.getTripById` (cần Trí đồng ý).
6. A1: đưa Bootstrap Icons về local.
7. C5: thêm 2 link navbar/sidebar.
8. B9: xuống dòng, bỏ `th:remove="all"` ở các template luồng đặt vé (nếu bạn muốn).
9. Chạy thật với SQL Server, chụp từng trang → chốt danh sách C6–C7.
10. Viết/cập nhật `DEMO_GUIDE.md`.

## 7. Cần bạn quyết định

1. **Bạn là thành viên nào (Khuyên / Trí / Tân / Trâm)?** `AGENTS.md` chỉ cho sửa file thuộc module của người dùng; nhiều mục trên thuộc người khác.
2. Các mục chạm **file chung hoặc URL** (B1, B12, A1 nếu đổi `layout.html`) có được làm không?
3. B9 (xuống dòng / xoá khối mẫu trong template): làm hay để nguyên?
4. B10: dòng comment đang sửa dở trong `PaymentService.java`: giữ, viết lại hay bỏ?
5. Có chạy được SQL Server trên máy này để Giai đoạn 2 kiểm tra luồng thật không?
