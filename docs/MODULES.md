# Chia việc 4 thành viên

| Thành viên | Backend | Giao diện (FE) |
|---|---|---|
| **BE1** | Tuyến & Bến: `Station*`, `Route*`, `RouteStation*` (ít việc, phần lớn đã có) | **Phụ trách FE chính**: layout chung, `app.css`, trang chủ, toàn bộ luồng đặt vé/thanh toán |
| **BE2** | Tàu, Ghế & Chuyến: `Waterbus*`, `Seat*`, `Trip*` | **Hỗ trợ UI**: trang kết quả tìm chuyến, sơ đồ chọn ghế, mẫu chung trang admin |
| **BE3** | Tài khoản, Loại vé & Khuyến mãi: `Account*`, `Auth*`, `TicketType*`, `Promotion*` | Tự làm trang đăng nhập/đăng ký + admin khuyến mãi/loại vé (copy mẫu admin của BE2) |
| **BE4** | Đặt vé & Thanh toán: `Booking*`, `Ticket*`, `Payment*` | Không - giao diện luồng đặt vé do BE1 làm |

**Ai sở hữu file nào** - một file chỉ một người sửa, nên không bao giờ conflict:

| File | Chủ |
|---|---|
| `templates/fragments/layout.html`, `static/css/app.css`, `static/js/**`, ảnh | BE1 |
| `templates/index.html`, `templates/booking/**`, `templates/payment/**`, `templates/station/**`, `templates/route/**` | BE1 |
| `templates/trip/**`, `templates/admin/**` (trừ 2 thư mục của BE3), `static/css/admin.css` | BE2 |
| `templates/auth/**`, `templates/admin/promotion/**`, `templates/admin/tickettype/**` | BE3 |
| Controller / Service / DTO / Repository của module | BE của module đó |
| `pom.xml`, `application.properties`, `entity/*`, `database/*`, `.github/*` | Leader |

Cột "FE" trong **[docs/PAGES.md](PAGES.md)** ghi rõ chủ của từng trang.
Ví dụ hoàn chỉnh để copy cách làm: **Station** (`StationController` → `StationService` →
`StationServiceImpl` → `StationRepository` → `Station`, view `templates/station/list.html`).

---

## KẾ HOẠCH TỚI DEMO: THỨ 5 22/10, 17:00

### Phạm vi demo

| Mức | Nội dung |
|---|---|
| **BẮT BUỘC** (luồng chính) | Tìm chuyến (bến đi, bến đến, ngày) → chọn ghế → nhập thông tin + mã khuyến mãi → tạo đơn → thanh toán MOCK → xem vé |
| **NÊN CÓ** | Đăng nhập/đăng ký, lịch sử đặt vé; admin: danh sách chuyến, CRUD khuyến mãi |
| **BỎ, làm sau demo** | CRUD bến/tuyến/tàu, trang tuyến, tự huỷ đơn quá hạn (`@Scheduled`), phân quyền admin, loại vé ưu tiên cần xác minh |

### Lịch

| Mốc | Việc |
|---|---|
| **T2 19/10, trước 12:00** | Leader merge PR chia việc. Cả nhóm setup xong, gửi ảnh `/stations` |
| **T2 19/10, 12:00-12:30** | Họp online chốt `PAGES.md` (tên attribute, field form). Sau họp bắt đầu code |
| **T2 19/10, tối** | Merge được: layout + `alerts` (BE1), `searchTrips` (BE2), `getPrice` + `calculateDiscount` (BE3) |
| **T3 20/10, trước 12:00** | Merge: trang chủ + kết quả tìm chuyến chạy với dữ liệu thật; BE4 tạo được booking từ dữ liệu cứng |
| **T3 20/10, tối** | Merge: sơ đồ ghế, trang checkout, đăng nhập |
| **T4 21/10, 18:00** | **Luồng chính chạy hết trên `develop`.** Ai xong sớm làm phần NÊN CÓ |
| **T5 22/10, 12:00** | **ĐÓNG CODE**: chỉ sửa lỗi, không thêm chức năng. Leader merge `develop` → `main` |
| **T5 22/10, 13:00-15:00** | Chạy thử trên **máy demo**: clone `main` mới, chạy lại script DB, đi đúng kịch bản demo 2 lần |
| **T5 22/10, 16:30** | Có mặt, mở sẵn app. 17:00 demo |

### Việc từng người, theo thứ tự ưu tiên

**BE1 - FE chính**
1. Layout: navbar, footer, màu/font trong `app.css`, fragment `alerts` (hiện `successMessage`/`errorMessage`). **Làm đầu tiên, mọi trang đều dùng**
2. Trang chủ có form tìm chuyến (`index`)
3. `booking/checkout` → `payment/index` → `booking/detail` (vé)
4. NÊN CÓ: `booking/history`, hiện tên người đăng nhập trên navbar

**BE2 - Tàu, Ghế & Chuyến + hỗ trợ UI**
1. `TripService.searchTrips` + trang `trip/index`
2. `getAvailableSeats`, `isSeatAvailable` + trang sơ đồ ghế `trip/seats` (lưới theo `seat_row`/`seat_col`, ghế đã đặt tô xám)
3. Mẫu admin `admin/_template/list.html` + `form.html` (xong trước T3 trưa để BE3 copy)
4. NÊN CÓ: admin danh sách chuyến

**BE3 - Tài khoản, Loại vé & Khuyến mãi**
1. **`TicketTypeService.getPrice` + `PromotionService.calculateDiscount` trước tiên** (BE4 cần)
2. Đăng nhập/đăng ký/đăng xuất, session `loginAccount`, BCrypt + 2 trang `auth/*`
3. NÊN CÓ: CRUD khuyến mãi (copy mẫu admin của BE2)

**BE4 - Đặt vé & Thanh toán**
1. `POST /booking/checkout` tính `subtotal`; `createBooking` (`@Transactional`): booking `PENDING`, vé `HOLD`
   - Khi BE2/BE3 chưa xong: tạm dùng giá cố định 15.000đ, giảm giá 0, ghế coi như trống
2. Thanh toán MOCK: payment `SUCCESS`, booking `PAID`, vé `VALID`, tăng `used_count` của khuyến mãi
3. Xem đơn theo mã (`booking/detail`)
4. NÊN CÓ: lịch sử đặt vé (`/my-bookings`)

### Luật riêng cho 3 ngày này
- **PR nhỏ, merge nhiều lần trong ngày.** Không giữ nhánh quá 1 ngày.
- Có PR mới → nhắn nhóm; người được nhờ **review trong vòng 2 giờ**. Chỉ cần chạy được + không sửa file của người khác là Approve.
- Bị chặn vì chờ người khác → làm tạm bằng dữ liệu cứng, ghi `// TODO` rồi đi tiếp, không ngồi chờ.
- **Kịch bản demo** (Leader viết trước T4): bến Bạch Đằng → Thanh Đa, ngày mai, chuyến đầu tiên, ghế B3-B4, mã `WELCOME10`, thanh toán MOCK, xem vé. Ai sửa lỗi cũng thử lại đúng kịch bản này.

---

## Cách FE và BE làm song song trên 1 trang

Tên URL, view, attribute và field của từng trang đã ghi trong **[docs/PAGES.md](PAGES.md)**.

1. **Chốt hợp đồng** trong buổi họp T2 12:00.
2. **Làm song song, mỗi người 1 nhánh, 1 file riêng:**
   - BE: viết Service + Controller đưa đúng attribute ra view. Chưa có view thật thì trả về view tạm có sẵn để test.
   - FE: viết file HTML với `th:*` dùng đúng tên trong hợp đồng. Chưa có BE thì để dữ liệu mẫu cứng trong thẻ
     (giống `index.html` trong project mẫu của thầy), Thymeleaf sẽ thay bằng dữ liệu thật khi BE xong.
3. **Ghép**: PR nào xong trước merge trước. Khi cả 2 đã vào `develop`, FE chạy thử luồng thật và sửa lặt vặt.

Dev giao diện cho nhanh: project có `spring-boot-devtools`. Sửa HTML/CSS xong:
VS Code tự build; IntelliJ bấm **Build → Build Project (Ctrl+F9)** rồi F5 trình duyệt, không cần restart app.

## Phụ thuộc giữa các module

```
BE4 Booking ──gọi──► BE2 TripService.getAvailableSeats / isSeatAvailable
            ──gọi──► BE3 TicketTypeService.getPrice, PromotionService.calculateDiscount
            ──đọc──► session "loginAccount" do BE3 đặt
BE2 Trip    ──dùng─► BE1 Station (danh sách bến cho form tìm chuyến)
Mọi trang   ──dùng─► BE1 layout.html (head, navbar, alerts, scripts)
```
Chữ ký các method "HỢP ĐỒNG" đã có sẵn, nên BE4 viết code gọi ngay từ đầu được;
khi chưa có code thật, method đó ném `UnsupportedOperationException("TODO BEx")`.
