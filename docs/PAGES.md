# Hợp đồng trang (FE ↔ BE)

Mỗi trang có người làm giao diện (cột **FE**, sở hữu file view) và người làm **BE** (sở hữu Controller).
Hai bên làm **song song** dựa trên bảng này, không ai sửa file của người kia:

- **BE** cam kết đưa ra view đúng **tên attribute** ở cột "Model".
- **FE** dùng đúng các tên đó trong `th:text`, `th:each`… và đặt `name` của input đúng như cột "Form gửi lên".
- Muốn đổi tên/thêm attribute → sửa bảng này trong PR của mình + nhắn người còn lại.

Tên dưới đây là **đề xuất**, chốt trong buổi họp T2 12:00. Quy ước: attribute camelCase,
danh sách số nhiều (`trips`), 1 đối tượng số ít (`trip`), tên field trong form = tên field của DTO.
Cột **Demo**: ★ = bắt buộc cho demo T5, ☆ = nên có, - = sau demo.

## Trang khách hàng

| Demo | URL | View | FE | BE | Model (BE → FE) | Form gửi lên (FE → BE) |
|---|---|---|---|---|---|---|
| ★ | `GET /` | `index` | BE1 | BE1 | `stations` | form tìm chuyến → `GET /trips`: `fromStationId`, `toStationId`, `date` |
| ★ | `GET /trips?fromStationId=&toStationId=&date=` | `trip/index` | BE2 | BE2 | `stations`, `trips` (mỗi chuyến: `id`, `tripCode`, `routeName`, `departureTime`, `arrivalTime`, `availableSeats`), giữ lại `fromStationId`, `toStationId`, `date` | |
| ★ | `GET /trips/{id}/seats?fromStationId=&toStationId=` | `trip/seats` | BE2 | BE2 | `trip`, `seats` (mỗi ghế: `id`, `seatCode`, `seatRow`, `seatCol`, `available`), `ticketTypes`, `fromStationId`, `toStationId` | → `POST /booking/checkout`: `tripId`, `seatIds` (nhiều checkbox), `ticketTypeId`, `fromStationId`, `toStationId` |
| ★ | `POST /booking/checkout` | `booking/checkout` | BE1 | BE4 | `trip`, `seats` (ghế đã chọn), `ticketType`, `subtotal`, các input ẩn giữ lại lựa chọn, `loginAccount` (nếu đã đăng nhập) | → `POST /booking`: các field trên + `contactName`, `contactPhone`, `contactEmail`, `promoCode` |
| ★ | `POST /booking` | redirect `/payment/{bookingCode}` | - | BE4 | lỗi validate → quay lại `booking/checkout` với `errorMessage` | |
| ★ | `GET /payment/{bookingCode}` | `payment/index` | BE1 | BE4 | `booking` (`bookingCode`, `subtotal`, `discountAmount`, `totalAmount`, `expiresAt`), `tickets` | → `POST /payment/{bookingCode}/mock` (nút "Thanh toán") |
| ★ | `GET /booking/{bookingCode}` | `booking/detail` | BE1 | BE4 | `booking`, `tickets` (mỗi vé: `ticketCode`, `seatCode`, `fromStation`, `toStation`, `price`, `status`) | |
| ☆ | `GET /my-bookings` | `booking/history` | BE1 | BE4 | `bookings` | |
| ☆ | `GET /auth/login` | `auth/login` | BE3 | BE3 | `errorMessage` (nếu sai) | → `POST /auth/login`: `email`, `password` |
| ☆ | `GET /auth/register` | `auth/register` | BE3 | BE3 | `errorMessage` | → `POST /auth/register`: `email`, `password`, `fullName`, `phone` |
| ☆ | `POST /auth/logout` | redirect `/` | BE1 (nút trên navbar) | BE3 | | |
| - | `GET /stations` | `station/list` | BE1 | BE1 | `stations` (đã làm, ví dụ mẫu) | |
| - | `GET /routes` | `route/index` | BE1 | BE1 | `routes`, `stopsByRoute` (Map routeId → danh sách bến) | |

Session: BE3 lưu tài khoản đăng nhập vào session với tên **`loginAccount`**. FE hiển thị tên trên navbar bằng
`${session.loginAccount}`; BE4 đọc để gắn `account_id` cho booking (chưa đăng nhập thì để NULL = khách vãng lai).

## Trang quản trị

BE2 làm mẫu chung `templates/admin/_template/list.html` (bảng + nút Thêm) và `form.html` (thêm/sửa)
trước **T3 20/10 trưa**; mỗi chủ trang copy mẫu sang thư mục của mình rồi sửa.

| Demo | URL | View | FE | BE | Model | Form gửi lên |
|---|---|---|---|---|---|---|
| ☆ | `GET /admin/trips…` | `admin/trip/list`, `…/form` | BE2 | BE2 | `trips` / `trip`, `routes`, `waterbuses` | `routeId`, `waterbusId`, `departureTime`, `status` |
| ☆ | `GET /admin/promotions…` | `admin/promotion/list`, `…/form` | BE3 | BE3 | `promotions` / `promotion` | `id` (rỗng = thêm mới), `code`, `name`, `discountType`, `discountValue`, `maxDiscount`, `minOrderAmount`, `startAt`, `endAt`, `usageLimit`, `isActive` |
| - | `GET /admin/ticket-types…` | `admin/tickettype/list`, `…/form` | BE3 | BE3 | `ticketTypes` / `ticketType` | `code`, `name`, `price`, `requiresVerification`, `description`, `isActive` |
| - | `GET /admin/waterbuses…` | `admin/waterbus/list`, `…/form` | BE2 | BE2 | `waterbuses` / `waterbus` | `code`, `name`, `capacity`, `status` |
| - | `GET /admin/stations…` | `admin/station/list`, `…/form` | BE2 | BE1 | `stations` / `station` | `code`, `name`, `address`, `isActive` |
| - | `GET /admin/routes…` | `admin/route/list`, `…/form` | BE2 | BE1 | `routes` / `route`, `stations` | `code`, `name`, `description`, `isActive`, `stationIds` (theo thứ tự) |

Thông báo sau khi lưu/xoá: BE dùng `RedirectAttributes.addFlashAttribute("successMessage", "...")`,
FE hiển thị `successMessage` / `errorMessage` qua fragment `alerts` trong `layout.html` (BE1 làm đầu tiên).
