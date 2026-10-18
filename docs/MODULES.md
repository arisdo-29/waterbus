# Chia việc 4 BE

Mỗi bạn sở hữu trọn 1 "lát cắt dọc": entity → repository → service → controller → view của module mình.
Khung code của cả 4 module **đã được tạo sẵn** (controller, service interface, service impl có `TODO`,
view `index.html`) - các bạn chỉ việc điền code, không phải tạo file chung.

Ví dụ hoàn chỉnh để copy cách làm: **Station** (`StationController` → `StationService` →
`StationServiceImpl` → `StationRepository` → `Station`, view `templates/station/list.html`).

---

## BE1 - Tuyến & Bến
**File:** `Station*`, `Route*`, `RouteStation*`, `templates/station/`, `templates/route/`
- [x] Danh sách bến, xem chi tiết bến (ví dụ mẫu)
- [ ] Trang tuyến: danh sách tuyến + các bến theo thứ tự (`RouteService.getStopsOfRoute`)
- [ ] Admin CRUD bến (`/admin/stations`), "xoá" = đặt `is_active = 0`
- [ ] Admin CRUD tuyến + sắp xếp bến của tuyến (`/admin/routes`)

## BE2 - Tàu, Ghế & Chuyến
**File:** `Waterbus*`, `Seat*`, `Trip*`, `templates/trip/`, `templates/waterbus/`
- [ ] Tìm chuyến: chọn bến đi, bến đến, ngày → danh sách chuyến (`TripService.searchTrips`)
- [ ] Sơ đồ ghế của chuyến, ghế đã đặt tô xám (`TripService.getAvailableSeats`) - **BE4 dùng**
- [ ] `TripService.isSeatAvailable` - **BE4 dùng**
- [ ] Admin: danh sách tàu, đổi trạng thái tàu; tạo/huỷ chuyến (`/admin/trips`)

## BE3 - Tài khoản, Loại vé & Khuyến mãi
**File:** `Account*`, `Auth*`, `TicketType*`, `Promotion*`, `templates/auth/`, `templates/tickettype/`, `templates/promotion/`
- [ ] Đăng ký, đăng nhập, đăng xuất (lưu account vào `HttpSession`), mật khẩu BCrypt
- [ ] Thống nhất với nhóm tên attribute session (VD `"loginAccount"`) để BE4 đọc
- [ ] Admin CRUD loại vé; `TicketTypeService.getPrice` - **BE4 dùng**
- [ ] Admin CRUD khuyến mãi; `PromotionService.calculateDiscount` - **BE4 dùng**
      (PERCENT có trần `max_discount`, FIXED, hạn dùng, `usage_limit`, `min_order_amount`)

## BE4 - Đặt vé & Thanh toán
**File:** `Booking*`, `Ticket*`, `Payment*`, `templates/booking/`, `templates/payment/`
- [ ] Chọn ghế + loại vé + nhập thông tin liên hệ + mã KM → tạo booking `PENDING`, vé `HOLD`,
      `expires_at = now + 10 phút` (`BookingService.createBooking`, `@Transactional`)
- [ ] Thanh toán MOCK: payment `SUCCESS`, booking `PAID`, vé `VALID`, tăng `used_count` của KM
- [ ] Tra cứu đơn theo mã, lịch sử đặt vé của khách (`/my-bookings`)
- [ ] (Nâng cao) `@Scheduled` huỷ booking quá hạn → vé `CANCELLED` để nhả ghế

---

## Phụ thuộc giữa các module

```
BE4 Booking ──gọi──► BE2 TripService.getAvailableSeats / isSeatAvailable
            ──gọi──► BE3 TicketTypeService.getPrice
            ──gọi──► BE3 PromotionService.calculateDiscount
            ──đọc──► session "loginAccount" do BE3 đặt
BE2 Trip    ──dùng─► BE1 Station/Route (chọn bến khi tìm chuyến)
```
Chữ ký các method "HỢP ĐỒNG" đã có sẵn, nên BE4 viết code gọi ngay từ đầu được;
khi chưa có code thật, method đó ném `UnsupportedOperationException("TODO BEx")`.

## Thứ tự gợi ý (2 tuần đầu)

| Tuần | BE1 | BE2 | BE3 | BE4 |
|---|---|---|---|---|
| 1 | Trang tuyến, CRUD bến | Tìm chuyến | Đăng ký/đăng nhập | Form đặt vé (giao diện), tạo booking với giá cố định |
| 2 | CRUD tuyến | Sơ đồ ghế + 2 hàm hợp đồng | Loại vé, khuyến mãi + 2 hàm hợp đồng | Nối hàm hợp đồng, thanh toán MOCK, lịch sử |
