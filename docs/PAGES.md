# Hợp đồng trang (FE ↔ BE)

Mỗi trang có người làm giao diện (cột **FE**, sở hữu file view) và người làm **BE** (sở hữu Controller).
Hai bên làm **song song** dựa trên bảng này, không ai sửa file của người kia:

- **BE** cam kết đưa ra view đúng **tên attribute** ở cột "Model".
- **FE** dùng đúng các tên đó trong `th:text`, `th:each`… và đặt `name` của input đúng như cột "Form gửi lên".
- Muốn đổi tên/thêm attribute → sửa bảng này trong PR của mình + nhắn người còn lại.

Quy ước: attribute camelCase, danh sách số nhiều (`trips`), 1 đối tượng số ít (`trip`),
tên field trong form = tên field của DTO. Cột **MS** = milestone (xem [MODULES.md](MODULES.md)).

Người: **Khuyên** (BE1) · **Trí** (BE2) · **Tân** (BE3) · **Trâm** (BE4)

## Trang khách hàng

| MS | URL | View | FE | BE | Model (BE → FE) | Form gửi lên (FE → BE) |
|---|---|---|---|---|---|---|
| M1 | `GET /` | `index` | Khuyên | Khuyên | `stations` | form tìm chuyến → `GET /trips`: `fromStationId`, `toStationId`, `date` |
| M1 | `GET /trips?fromStationId=&toStationId=&date=` | `trip/index` | Trí | Trí | `stations`, `trips` (mỗi chuyến: `id`, `tripCode`, `routeName`, `departureTime`, `arrivalTime`, `availableSeats`), giữ lại `fromStationId`, `toStationId`, `date` | |
| M1 | `GET /trips/{id}/seats?fromStationId=&toStationId=` | `trip/seats` | Trí | Trí | `trip`, `seats` (mỗi ghế: `id`, `seatCode`, `seatRow`, `seatCol`, `available`), `ticketTypes`, `fromStationId`, `toStationId` | → `POST /booking/checkout`: `tripId`, `seatIds` (nhiều checkbox), `ticketTypeId`, `fromStationId`, `toStationId` |
| M1 | `POST /booking/checkout` | `booking/checkout` | Khuyên | Trâm | `trip`, `seats` (ghế đã chọn), `ticketType`, `subtotal`, các input ẩn giữ lại lựa chọn, `loginAccount` (M2) | → `POST /booking`: các field trên + `contactName`, `contactPhone`, `contactEmail`, `promoCode` |
| M1 | `POST /booking` | redirect `/payment/{bookingCode}` | - | Trâm | lỗi validate → quay lại `booking/checkout` với `errorMessage` | |
| M1 | `GET /payment/{bookingCode}` | `payment/index` | Khuyên | Trâm | `booking` (`bookingCode`, `subtotal`, `discountAmount`, `totalAmount`, `expiresAt`), `tickets` | → `POST /payment/{bookingCode}/mock` (nút "Thanh toán") |
| M1 | `GET /booking/{bookingCode}` | `booking/detail` | Khuyên | Trâm | `booking`, `tickets` (mỗi vé: `ticketCode`, `seatCode`, `fromStation`, `toStation`, `price`, `status`) | |
| M2 | `GET /auth/login` | `auth/login` | Tân | Tân | `errorMessage` (nếu sai) | → `POST /auth/login`: `email`, `password` |
| M2 | `GET /auth/register` | `auth/register` | Tân | Tân | `errorMessage` | → `POST /auth/register`: `email`, `password`, `fullName`, `phone` |
| M2 | `POST /auth/logout` | redirect `/` | Khuyên (nút trên navbar) | Tân | | |
| M2 | `GET /my-bookings` | `booking/history` | Khuyên | Trâm | `bookings` | |
| M3 | `GET /routes` | `route/index` | Khuyên | Khuyên | `routes`, `stopsByRoute` (Map routeId → danh sách bến) | |
| ✔ | `GET /stations` | `station/list` | Khuyên | Khuyên | `stations` (đã làm, ví dụ mẫu) | |

Session (M2): Tân lưu tài khoản đăng nhập vào session với tên **`loginAccount`**. Khuyên hiển thị tên trên navbar bằng
`${session.loginAccount}`; Trâm đọc để gắn `account_id` cho booking (chưa đăng nhập thì để NULL = khách vãng lai).

## Trang quản trị (M3)

Trí làm mẫu chung `templates/admin/_template/list.html` (bảng + nút Thêm) và `form.html` (thêm/sửa)
đầu tiên trong M3; mỗi chủ trang copy mẫu sang thư mục của mình rồi sửa.

| MS | URL | View | FE | BE | Model | Form gửi lên |
|---|---|---|---|---|---|---|
| M3 | `GET /admin/trips…` | `admin/trip/list`, `…/form` | Trí | Trí | `trips` / `trip`, `routes`, `waterbuses` | `routeId`, `waterbusId`, `departureTime`, `status` |
| M3 | `GET /admin/waterbuses…` | `admin/waterbus/list`, `…/form` | Trí | Trí | `waterbuses` / `waterbus` | `code`, `name`, `capacity`, `status` |
| M3 | `GET /admin/stations…` | `admin/station/list`, `…/form` | Trí | Khuyên | `stations` / `station` | `id` (rỗng = thêm mới), `code`, `name`, `address`, `isActive` |
| M3 | `GET /admin/routes…` | `admin/route/list`, `…/form` | Trí | Khuyên | `routes` / `route`, `stations` | `code`, `name`, `description`, `isActive`, `stationIds` (theo thứ tự) |
| M3 | `GET /admin/promotions…` | `admin/promotion/list`, `…/form` | Tân | Tân | `promotions` / `promotion` | `id`, `code`, `name`, `discountType`, `discountValue`, `maxDiscount`, `minOrderAmount`, `startAt`, `endAt`, `usageLimit`, `isActive` |
| M3 | `GET /admin/ticket-types…` | `admin/tickettype/list`, `…/form` | Tân | Tân | `ticketTypes` / `ticketType` | `code`, `name`, `price`, `requiresVerification`, `description`, `isActive` |

Thông báo sau khi lưu/xoá: BE dùng `RedirectAttributes.addFlashAttribute("successMessage", "...")`,
FE hiển thị `successMessage` / `errorMessage` qua fragment `alerts` trong `layout.html` (Khuyên làm ở M1).
