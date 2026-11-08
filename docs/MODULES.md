# Chia việc & Milestone

## Thành viên

| Vai trò | Người | Backend | Giao diện (FE) |
|---|---|---|---|
| **BE1** | **Khuyên** | Tuyến & Bến: `Station*`, `Route*`, `RouteStation*` | **FE chính**: layout, trang chủ, checkout, thanh toán, xem vé |
| **BE2** | **Trí** | Tàu, Ghế & Chuyến: `Waterbus*`, `Seat*`, `Trip*` | UI tìm chuyến, sơ đồ chọn ghế, mẫu trang admin |
| **BE3** | **Tân** | Tài khoản, Loại vé & Khuyến mãi: `Account*`, `Auth*`, `TicketType*`, `Promotion*` | Trang đăng nhập/đăng ký, admin khuyến mãi |
| **BE4** | **Trâm** | Đặt vé & Thanh toán: `Booking*`, `Ticket*`, `Payment*` | Không (giao diện luồng đặt vé do Khuyên làm) |

## Ai sở hữu file nào

Một file chỉ một người sửa, nên không bao giờ conflict. Cột **FE** trong [docs/PAGES.md](PAGES.md) ghi chủ từng trang.

| File | Chủ |
|---|---|
| `templates/fragments/layout.html`, `static/css/app.css`, `static/js/**`, ảnh | Khuyên (BE1) |
| `templates/index.html`, `templates/booking/**`, `templates/payment/**`, `templates/station/**`, `templates/route/**` | Khuyên (BE1) |
| `templates/trip/**`, `templates/admin/**` (trừ các thư mục của Tân), `static/css/admin.css` | Trí (BE2) |
| `templates/auth/**`, `templates/admin/promotion/**`, `templates/admin/tickettype/**`, `templates/admin/checkin/**`, `templates/admin/account/**` | Tân (BE3) |
| Controller / Service / DTO / Repository của module | BE của module đó |
| `pom.xml`, `application.properties`, `entity/*`, `database/*`, `.github/*` | Leader |

Ví dụ hoàn chỉnh để copy cách làm: **Station** (`StationController` → `StationService` →
`StationServiceImpl` → `StationRepository` → `Station`, view `templates/station/list.html`).

---

## Milestone

Mỗi dòng checkbox = 1 Issue trên GitHub, gắn đúng Milestone và người làm.
Làm xong milestone trước rồi mới sang milestone sau. Chỉ **M1** có hạn.

### M1 - Demo luồng đặt vé · HẠN: Thứ 5 22/10/2026, 17:00

**Mục tiêu:** đi trọn luồng *tìm chuyến → chọn ghế → nhập thông tin + mã khuyến mãi → tạo đơn → thanh toán MOCK → xem vé*
bằng dữ liệu mẫu trong script database.

**Khuyên (BE1)**
- [x] Layout chung: navbar, footer, màu/font trong `app.css`, fragment `alerts` (hiện `successMessage`/`errorMessage`) - **làm đầu tiên, mọi trang đều dùng**
- [x] Trang chủ có form tìm chuyến (`index`) + `StationService` trả danh sách bến cho form
- [x] Trang checkout (`booking/checkout`)
- [x] Trang thanh toán (`payment/index`)
- [x] Trang xem vé (`booking/detail`)

Kiểm chứng BE1: HomeStationTests gọi `/` qua MVC/service/H2, lọc bến inactive, thứ tự ổn định và danh sách rỗng.
CustomerViewTests render Thymeleaf thật cho các trang trên, alerts an toàn, giữ lựa chọn/liên hệ và trạng thái đơn.
Đã chạy JAR sau `mvnw.cmd verify` với SQL Server local: `/` trả HTTP 200, hiển thị 5 bến;
trình duyệt 360px/1280px không tràn ngang, menu mobile mở được, không có lỗi JavaScript.
Giao diện #5–#7 đã có từ PR #46; việc đánh dấu ở đây chỉ xác nhận phần FE, còn chờ backend BE4 #13–#16.

**Trí (BE2)**
- [x] `TripService.searchTrips` + trang kết quả tìm chuyến (`trip/index`)
- [x] `TripService.getAvailableSeats`, `isSeatAvailable` (hợp đồng cho Trâm)
- [x] Trang sơ đồ chọn ghế (`trip/seats`): lưới theo `seat_row`/`seat_col`, ghế đã đặt tô xám

**Tân (BE3)**
- [x] `TicketTypeService.getPrice` (hợp đồng cho Trâm) - **làm trước tiên**
- [x] `PromotionService.calculateDiscount` (hợp đồng cho Trâm): PERCENT có trần `max_discount`, FIXED, hạn dùng, `usage_limit`, `min_order_amount` - **làm trước tiên**

**Trâm (BE4)**
- [x] `POST /booking/checkout`: nhận lựa chọn ghế, tính `subtotal`
- [x] `BookingService.createBooking` (`@Transactional`): booking `PENDING`, vé `HOLD`, `expires_at = now + 10 phút`
- [x] `PaymentService.payMock`: payment `SUCCESS`, booking `PAID`, vé `VALID`, tăng `used_count` của khuyến mãi
- [x] Xem đơn theo mã (`GET /booking/{bookingCode}`)

Kiểm chứng BE4 #13–#16 (PR #55, #56, #58, #59): BookingCreateTest, PaymentMockTest chạy luồng checkout → tạo đơn →
thanh toán → xem vé bằng MockMvc/H2. Đã chạy tay trên SQL Server 2022 local: đơn `PENDING` 2 vé `HOLD`, hạn 10 phút,
`WELCOME10` giảm 3.000đ; thanh toán xong đơn `PAID`, vé `VALID`, `used_count` +1.

**Cả nhóm / Leader**
- [ ] Setup máy, chạy được `/stations`
- [x] Viết kịch bản demo: Bạch Đằng → Thanh Đa, ngày mai, chuyến đầu tiên, ghế B3-B4, mã `WELCOME10`, thanh toán MOCK, xem vé
- [ ] Chạy thử trên máy demo (clone `main` mới + chạy lại script database)

**Các mốc trong M1**

| Khi nào | Việc |
|---|---|
| T2 19/10, 12:00 | Họp online chốt `PAGES.md` (tên attribute, field form) |
| T4 21/10, 18:00 | Luồng chính chạy hết trên `develop` |
| T5 22/10, 12:00 | **Đóng code**: chỉ sửa lỗi. Leader merge `develop` → `main` |
| T5 22/10, 13:00-15:00 | Chạy thử trên máy demo, đi kịch bản 2 lần |
| T5 22/10, 17:00 | **Demo** |

Bị chặn vì chờ module khác → dùng dữ liệu cứng + `// TODO` rồi đi tiếp
(VD Trâm tạm dùng giá 15.000đ, giảm 0đ, ghế coi như trống cho tới khi Trí/Tân merge).

### M2 - Tài khoản & lịch sử đặt vé

**Tân (BE3)**
- [x] Đăng ký, đăng nhập, đăng xuất: session `loginAccount`, mật khẩu BCrypt
- [x] Trang `auth/login`, `auth/register`

**Khuyên (BE1)**
- [x] Navbar hiện tên người đăng nhập + nút đăng xuất
- [x] Trang lịch sử đặt vé (`booking/history`)

Kiểm chứng BE1: render với session AccountDTO cho khách/CUSTOMER/ADMIN/STAFF và role lạ;
history rỗng/có đơn, bảng desktop/card mobile, trạng thái PENDING/PAID/EXPIRED/CANCELLED.
Script `src/test/js/customer-views.browser.cjs` kiểm tra HTML fixture đã render ở 360px/1280px,
đổi chiều/chặn trùng bến và countdown. Chạy sau Maven test, dùng Playwright đã cài sẵn qua `PLAYWRIGHT_MODULE`.
Giao diện #23 đã có từ PR #46; backend lịch sử/tài khoản/checkout #24–#26 vẫn thuộc Trâm và chưa được xác nhận hoàn thành.

**Trâm (BE4)**
- [x] Gắn `account_id` cho booking khi đã đăng nhập (chưa đăng nhập = khách vãng lai)
- [x] `GET /my-bookings`: lịch sử đặt vé của khách
- [x] Checkout tự điền tên/điện thoại/email từ tài khoản đang đăng nhập

Kiểm chứng BE4 #24–#26 (PR #61, #62, #63): test đặt vé có/không có session `loginAccount`, `/my-bookings`
chuyển sang `/auth/login` khi chưa đăng nhập, chỉ hiện đơn của mình, mới nhất lên đầu. Tự điền (#26) do template
checkout đọc `session.loginAccount`, BE4 chỉ thêm test xác nhận.

### M3 - Quản trị

**Trí (BE2)**
- [x] Mẫu chung trang admin `admin/_template/list.html` + `form.html` - **làm đầu tiên trong M3**
- [x] Admin chuyến: danh sách, tạo chuyến, huỷ chuyến
- [x] Admin tàu: danh sách, đổi trạng thái (ACTIVE/MAINTENANCE/INACTIVE)
- [x] Giao diện admin bến và tuyến (backend do Khuyên làm)

**Tân (BE3)**
- [x] CRUD khuyến mãi + trang `admin/promotion/*`
- [x] CRUD loại vé + trang `admin/tickettype/*`
- [x] Phân quyền: chỉ ADMIN/STAFF vào được `/admin/**`

**Khuyên (BE1)**
- [x] Backend CRUD bến (ẩn bến = `is_active = 0`)
- [x] Backend CRUD tuyến + sắp xếp thứ tự bến
- [x] Trang tuyến cho khách (`route/index`)

BE1 #34–#36: StationRouteAdminTests render đúng template admin hiện có, kiểm tra thêm/sửa/ẩn,
code trùng, ID sai, checkbox marker, validation và giữ dữ liệu khi lỗi. Thứ tự tuyến 1..n được tải lại đúng,
giữ ID/số phút của bến còn lại, kiểm tra bến trùng/không tồn tại/inactive; RouteTransactionTests kiểm tra rollback giữa lần lưu.
GET `/routes` dùng DTO và chỉ lấy tuyến active; có test rỗng/có dữ liệu và đúng thứ tự.
SQL Server local chỉ kiểm tra đọc: `/routes` có 4 tuyến, `/stations` có 5 bến; trang/form admin render được.
Không ghi/reset database dùng chung; test CRUD/constraint bằng H2 không thay thế kiểm chứng đầy đủ SQL Server.
Endpoint admin chưa được xác minh bảo vệ truy cập, cần Tân tích hợp #33 trước khi phát hành.

### M4 - Hoàn thiện

**Trâm (BE4)**
- [x] `@Scheduled` tự huỷ booking quá hạn → vé `CANCELLED` để nhả ghế
- [x] Huỷ đơn chưa thanh toán (backend)

Kiểm chứng BE4 #37–#38 (PR #69): BookingExpiryCancelTest kiểm tra job chỉ huỷ đơn `PENDING` quá hạn và nhả ghế cho
người khác đặt lại, huỷ đơn kiểm tra không tìm thấy / không còn `PENDING` / sai chủ đơn.
Giao diện đã nối: nút "Huỷ đơn" trên `booking/detail` (#77, có pop-up xác nhận) và `th:object`/`th:errors` trong `booking/checkout`.

**Tân (BE3)**
- [x] Soát vé (STAFF): nhập `ticket_code` → vé `USED`, ghi `checked_in_at`

**Trí (BE2)**
- [x] Kiểm tra trùng ghế khi 2 người đặt cùng lúc (bắt lỗi unique index `UX_ticket_trip_seat_active`, báo "ghế vừa có người đặt")

**Khuyên (BE1)**
- [ ] Trang lỗi 404/500, giao diện điện thoại, rà soát lại toàn bộ giao diện

BE1 #41: đã kiểm tra 404/500 qua error dispatch thật của Spring Boot (500 chỉ được tạo trong test),
tiếng Việt và không lộ exception; sửa nút tải lại 500, bổ sung alerts vào hai trang lỗi.
Đã rà 15 trang BE1/admin bằng HTML render thật ở 360/768/1280px, không tràn ngang/lỗi JS;
kiểm tra navbar, lọc bến và tải lại trang lỗi bằng `src/test/js/be1-admin.browser.cjs`.
Giữ issue #41 mở cho phần rà toàn bộ giao diện sau khi các trang module khác và luồng BE4 được nối thật;
checkout/payment/detail/history hiện được kiểm tra bằng fixture của PR #50, `/booking` vẫn là trang khung BE4.
Phần #42 của BE1: @Valid/BindingResult và alerts tiếng Việt đã có cho form bến/tuyến; template admin của Trí
đã có `th:errors` dưới từng field (kiểm tra lại 08/11/2026). Hợp đồng/hành vi ẩn tuyến ở PAGES.md.

**Cả nhóm**
- [x] Validate form (`@Valid`, hiện lỗi dưới từng ô)
- [ ] Mỗi người tự code lại 1 chức năng CRUD trên IntelliJ không dùng AI (luyện thi PE)

### M5 - Vé ưu đãi, thanh toán VNPay, hoàn tiền, soát vé nhanh

Script DB: `database/changes/2026-11-08_db_hanh-khach-soat-ve-hoan-tien.sql` (ticket.passenger_name/birth_year,
bảng `ticket_scan_log`, bảng `refund`, trip.cancel_reason). Hợp đồng chi tiết ở [PAGES.md](PAGES.md).

**Khuyên (BE1, FE chính)**
- [x] Pop-up xác nhận dùng chung (`data-confirm`, thay `confirm()` của trình duyệt) - hướng dẫn ở CONTRIBUTING.md mục 8
- [x] Script database + entity `TicketScanLog`, `Refund`, cột mới của `Ticket`, `Trip`
- [x] Menu quản trị: Doanh thu, Hoàn tiền, Tài khoản (ADMIN); Soát vé đầu danh sách (STAFF)
- [x] Checkout vé ưu đãi: họ tên + năm sinh từng ghế, kiểm tra tuổi bằng JS, ô `acceptPolicy` + modal chính sách, đơn 0 đ
- [x] Trang vé: tên hành khách, nhãn giấy tờ gốc, QR mã đơn, banner chờ hoàn / đã hoàn tiền, phương thức thanh toán
- [x] Lịch sử: nhãn "Chờ hoàn tiền" / "Đã hoàn tiền"
- [x] Trang thanh toán chọn Mô phỏng / VNPay; DEMO_GUIDE mục VNPay Sandbox
- [x] JS + CSS + thư viện html5-qrcode cho màn soát vé (`initCheckin`, fragment `scriptsCheckin`)

**Trâm (BE4) - còn chờ để khớp giao diện**
- [ ] `BookingForm` thêm `passengerNames`, `birthYears` (List, cùng thứ tự `seatIds`), `acceptPolicy` (bắt buộc true);
      kiểm tra năm sinh 1900..năm nay, `SENIOR_70` ≥ 70 tuổi, `PRIORITY` ≥ 13 tuổi (tính theo năm của chuyến);
      lỗi gắn field `passengerNames[i]` / `birthYears[i]` / `acceptPolicy`; lưu vào `ticket.passenger_name`, `birth_year`
- [ ] Đơn tổng 0 đ: xác nhận ngay (`PAID`, vé `VALID`) và redirect `/booking/{bookingCode}`, không qua `/payment`
- [ ] `TicketDTO` thêm `passengerName`, `birthYear`, `requiresVerification`; `BookingDTO` thêm `refundStatus`
- [ ] `GET /booking/{bookingCode}` thêm model `refund`, `tripCancelReason`, `payment` (`method`, `transactionRef`)
- [ ] `POST /payment/{bookingCode}/vnpay`, `GET /payment/vnpay-return`; chốt tên khoá `vnpay.*` trong DEMO_GUIDE
- [ ] Huỷ chuyến → đơn đã trả tiền tạo `refund` PENDING; backend `/admin/revenue`, `/admin/refunds` (hoàn thủ công → `REFUNDED`)
- [ ] Repository/Service cho entity `Refund`

**Tân (BE3) - còn chờ để khớp giao diện**
- [ ] `admin/checkin/index.html` gắn data-attribute theo PAGES.md "Soát vé – hợp đồng JS" và dùng fragment `scriptsCheckin`
- [ ] `GET /admin/checkin?ticketCode=` tra mã vé hoặc mã đơn → modal `data-checkin-preview`; POST xong → `data-checkin-result`
- [ ] Repository/Service cho entity `TicketScanLog` (ghi mỗi lần VIEW/CHECKIN/REJECT)
- [ ] Trang `/admin/accounts` (BE + FE) và chặn chỉ ADMIN cho `/admin/revenue`, `/admin/refunds`, `/admin/accounts`
- [ ] Thêm `data-confirm` cho form soát vé, lưu khuyến mãi/loại vé (`danger` cho nút ẩn/xoá)

**Trí (BE2) - còn chờ để khớp giao diện**
- [ ] Huỷ chuyến nhập và lưu `Trip.cancelReason`
- [ ] Giao diện `admin/revenue`, `admin/refund` (FE theo hợp đồng Trâm chốt trong PAGES.md)
- [ ] Thêm `data-confirm` cho form lưu chuyến/tàu/bến/tuyến, đổi trạng thái tàu; `danger` cho nút huỷ/ngừng hoạt động

---

## Cách FE và BE làm song song trên 1 trang

Tên URL, view, attribute và field của từng trang đã ghi trong **[docs/PAGES.md](PAGES.md)**.

1. **Chốt hợp đồng** giữa người làm BE và người làm FE của trang.
2. **Làm song song, mỗi người 1 nhánh, 1 file riêng:**
   - BE: viết Service + Controller đưa đúng attribute ra view. Chưa có view thật thì trả về view tạm có sẵn để test.
   - FE: viết file HTML với `th:*` dùng đúng tên trong hợp đồng. Chưa có BE thì để dữ liệu mẫu cứng trong thẻ
     (giống `index.html` trong project mẫu của thầy), Thymeleaf sẽ thay bằng dữ liệu thật khi BE xong.
3. **Ghép**: PR nào xong trước merge trước. Khi cả 2 đã vào `develop`, FE chạy thử luồng thật và sửa lặt vặt.

Dev giao diện cho nhanh: project có `spring-boot-devtools`. Sửa HTML/CSS xong:
VS Code tự build; IntelliJ bấm **Build → Build Project (Ctrl+F9)** rồi F5 trình duyệt, không cần restart app.

## Phụ thuộc giữa các module

```
Trâm (Booking) ──gọi──► Trí: TripService.getAvailableSeats / isSeatAvailable
               ──gọi──► Tân: TicketTypeService.getPrice, PromotionService.calculateDiscount
               ──đọc──► session "loginAccount" do Tân đặt (M2)
Trí (Trip)     ──dùng─► Khuyên: StationService (danh sách bến cho form tìm chuyến)
Mọi trang      ──dùng─► Khuyên: layout.html (head, navbar, alerts, scripts)
```
Chữ ký các method "HỢP ĐỒNG" đã có sẵn, nên Trâm viết code gọi ngay từ đầu được;
khi chưa có code thật, method đó ném `UnsupportedOperationException("TODO BEx")`.
