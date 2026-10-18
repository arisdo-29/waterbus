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
| `templates/trip/**`, `templates/admin/**` (trừ 2 thư mục của Tân), `static/css/admin.css` | Trí (BE2) |
| `templates/auth/**`, `templates/admin/promotion/**`, `templates/admin/tickettype/**` | Tân (BE3) |
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
- [ ] Layout chung: navbar, footer, màu/font trong `app.css`, fragment `alerts` (hiện `successMessage`/`errorMessage`) - **làm đầu tiên, mọi trang đều dùng**
- [ ] Trang chủ có form tìm chuyến (`index`) + `StationService` trả danh sách bến cho form
- [ ] Trang checkout (`booking/checkout`)
- [ ] Trang thanh toán (`payment/index`)
- [ ] Trang xem vé (`booking/detail`)

**Trí (BE2)**
- [ ] `TripService.searchTrips` + trang kết quả tìm chuyến (`trip/index`)
- [ ] `TripService.getAvailableSeats`, `isSeatAvailable` (hợp đồng cho Trâm)
- [ ] Trang sơ đồ chọn ghế (`trip/seats`): lưới theo `seat_row`/`seat_col`, ghế đã đặt tô xám

**Tân (BE3)**
- [ ] `TicketTypeService.getPrice` (hợp đồng cho Trâm) - **làm trước tiên**
- [ ] `PromotionService.calculateDiscount` (hợp đồng cho Trâm): PERCENT có trần `max_discount`, FIXED, hạn dùng, `usage_limit`, `min_order_amount` - **làm trước tiên**

**Trâm (BE4)**
- [ ] `POST /booking/checkout`: nhận lựa chọn ghế, tính `subtotal`
- [ ] `BookingService.createBooking` (`@Transactional`): booking `PENDING`, vé `HOLD`, `expires_at = now + 10 phút`
- [ ] `PaymentService.payMock`: payment `SUCCESS`, booking `PAID`, vé `VALID`, tăng `used_count` của khuyến mãi
- [ ] Xem đơn theo mã (`GET /booking/{bookingCode}`)

**Cả nhóm / Leader**
- [ ] Setup máy, chạy được `/stations`
- [ ] Viết kịch bản demo: Bạch Đằng → Thanh Đa, ngày mai, chuyến đầu tiên, ghế B3-B4, mã `WELCOME10`, thanh toán MOCK, xem vé
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
- [ ] Đăng ký, đăng nhập, đăng xuất: session `loginAccount`, mật khẩu BCrypt
- [ ] Trang `auth/login`, `auth/register`

**Khuyên (BE1)**
- [ ] Navbar hiện tên người đăng nhập + nút đăng xuất
- [ ] Trang lịch sử đặt vé (`booking/history`)

**Trâm (BE4)**
- [ ] Gắn `account_id` cho booking khi đã đăng nhập (chưa đăng nhập = khách vãng lai)
- [ ] `GET /my-bookings`: lịch sử đặt vé của khách
- [ ] Checkout tự điền tên/điện thoại/email từ tài khoản đang đăng nhập

### M3 - Quản trị

**Trí (BE2)**
- [ ] Mẫu chung trang admin `admin/_template/list.html` + `form.html` - **làm đầu tiên trong M3**
- [ ] Admin chuyến: danh sách, tạo chuyến, huỷ chuyến
- [ ] Admin tàu: danh sách, đổi trạng thái (ACTIVE/MAINTENANCE/INACTIVE)
- [ ] Giao diện admin bến và tuyến (backend do Khuyên làm)

**Tân (BE3)**
- [ ] CRUD khuyến mãi + trang `admin/promotion/*`
- [ ] CRUD loại vé + trang `admin/tickettype/*`
- [ ] Phân quyền: chỉ ADMIN/STAFF vào được `/admin/**`

**Khuyên (BE1)**
- [ ] Backend CRUD bến (ẩn bến = `is_active = 0`)
- [ ] Backend CRUD tuyến + sắp xếp thứ tự bến
- [ ] Trang tuyến cho khách (`route/index`)

### M4 - Hoàn thiện

**Trâm (BE4)**
- [ ] `@Scheduled` tự huỷ booking quá hạn → vé `CANCELLED` để nhả ghế
- [ ] Huỷ đơn chưa thanh toán

**Tân (BE3)**
- [ ] Soát vé (STAFF): nhập `ticket_code` → vé `USED`, ghi `checked_in_at`

**Trí (BE2)**
- [ ] Kiểm tra trùng ghế khi 2 người đặt cùng lúc (bắt lỗi unique index `UX_ticket_trip_seat_active`, báo "ghế vừa có người đặt")

**Khuyên (BE1)**
- [ ] Trang lỗi 404/500, giao diện điện thoại, rà soát lại toàn bộ giao diện

**Cả nhóm**
- [ ] Validate form (`@Valid`, hiện lỗi dưới từng ô)
- [ ] Mỗi người tự code lại 1 chức năng CRUD trên IntelliJ không dùng AI (luyện thi PE)

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
