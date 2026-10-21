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
| M1 | `GET /` | `index` | Khuyên | Khuyên | `stations`: StationDTO đang hoạt động, tăng dần theo `id`, qua `StationService.getActiveStations()` | form tìm chuyến → `GET /trips`: `fromStationId`, `toStationId`, `date` |
| M1 | `GET /trips?fromStationId=&toStationId=&date=` | `trip/index` | Trí | Trí | `stations`, `trips` (mỗi chuyến: `id`, `tripCode`, `routeName`, `departureTime`, `arrivalTime`, `availableSeats`), giữ lại `fromStationId`, `toStationId`, `date` | |
| M1 | `GET /trips/{id}/seats?fromStationId=&toStationId=` | `trip/seats` | Trí | Trí | `trip`, `seats` (mỗi ghế: `id`, `seatCode`, `seatRow`, `seatCol`, `available`), `ticketTypes`, `fromStationId`, `toStationId` | → `POST /booking/checkout`: `tripId`, `seatIds` (nhiều checkbox), `ticketTypeId`, `fromStationId`, `toStationId` |
| M1 | `POST /booking/checkout` | `booking/checkout` | Khuyên | Trâm | `trip`, `seats` (ghế đã chọn), `ticketType`, `subtotal`, `fromStation`, `toStation`, `fromStationId`, `toStationId`; tài khoản từ session `loginAccount` (M2) | → `POST /booking`: `tripId`, `ticketTypeId`, `fromStationId`, `toStationId`, `seatIds` (nhiều input cùng tên), `contactName`, `contactPhone`, `contactEmail`, `promoCode` |
| M1 | `POST /booking` | redirect `/payment/{bookingCode}` | - | Trâm | lỗi validate → render lại `booking/checkout` với `errorMessage`, đầy đủ model checkout và request parameters đã nhập | |
| M1 | `GET /payment/{bookingCode}` | `payment/index` | Khuyên | Trâm | `booking` (`bookingCode`, `status`, `subtotal`, `discountAmount`, `totalAmount`, `expiresAt`), `trip`, `tickets` | → `POST /payment/{bookingCode}/mock` (nút "Thanh toán") |
| M1 | `GET /booking/{bookingCode}` | `booking/detail` | Khuyên | Trâm | `booking` (`bookingCode`, `status`, `contactName`, `contactPhone`, `contactEmail`, `totalAmount`, `createdAt`, `paidAt`), `trip`, `tickets` (mỗi vé: `ticketCode`, `seatCode`, `fromStation`, `toStation`, `price`, `status`) | |
| M2 | `GET /auth/login` | `auth/login` | Tân | Tân | `errorMessage` (nếu sai) | → `POST /auth/login`: `email`, `password` |
| M2 | `GET /auth/register` | `auth/register` | Tân | Tân | `errorMessage` | → `POST /auth/register`: `email`, `password`, `fullName`, `phone` |
| M2 | `POST /auth/logout` | redirect `/` | Khuyên (nút trên navbar) | Tân | | |
| M2 | `GET /my-bookings` | `booking/history` | Khuyên | Trâm | `bookings`: mỗi đơn có `bookingCode`, `createdAt`, `totalAmount`, `status`; danh sách rỗng khi chưa có đơn | |
| M3 | `GET /routes` | `route/index` | Khuyên | Khuyên | `routes`: RouteDTO active (`id`, `code`, `name`, `description`, `isActive`); `stopsByRoute`: Map routeId → List StationDTO theo `stopOrder` | |
| ✔ | `GET /stations` | `station/list` | Khuyên | Khuyên | `stations` (đã làm, ví dụ mẫu) | |

Session (M2): Tân lưu tài khoản đăng nhập vào session với tên **`loginAccount`**. Khuyên hiển thị tên trên navbar bằng
`${session.loginAccount}` (AccountDTO: `id`, `fullName`, `email`, `phone`, `role`, `status`);
Trâm đọc để gắn `account_id` cho booking (chưa đăng nhập thì để NULL = khách vãng lai).
Navbar chỉ hiện link quản trị cho `ADMIN`/`STAFF`; phân quyền endpoint do BE3 xử lý riêng.

Chi tiết model FE M1–M2: `trip` có `id`, `routeName`, `tripCode`, `departureTime`, `arrivalTime`;
`seats` có `id`, `seatCode`; `ticketType` có `id`, `name`, `price`; `fromStation`/`toStation` có `name`.
Trong `tickets`, `fromStation`/`toStation` là chuỗi tên bến, `price` là BigDecimal.
Các số tiền model là BigDecimal; thời gian là LocalDateTime (có thể null khi chưa có).
Payment chỉ cho bấm khi `PENDING` và có hạn giữ chỗ; JS vô hiệu hóa nút nếu hạn sai/đã qua,
không đổi trạng thái đơn. Checkout ưu tiên request parameters khi render lỗi, kể cả giá trị rỗng;
BE4 cần forward/render lại cùng request và bổ sung đầy đủ model, không redirect làm mất dữ liệu.

**Trạng thái tích hợp tại `49bca30`:** `/` đã nối service/database trong phần BE1 này.
BE3 đã có handler login/register/logout và AccountDTO trong session. Các hợp đồng booking/payment/detail/history
ở trên là yêu cầu FE, đã kiểm tra bằng fixture render trong test; BE4 hiện chưa triển khai các endpoint đó
(controller mới có `GET /booking`, `GET /payment`). Trí còn cần nối dữ liệu tìm chuyến/chọn ghế.
Không coi kiểm thử fixture là kiểm thử toàn luồng hoặc hoàn thành #24–#26.

## Trang quản trị (M3)

Trí làm mẫu chung `templates/admin/_template/list.html` (bảng + nút Thêm) và `form.html` (thêm/sửa)
đầu tiên trong M3; mỗi chủ trang copy mẫu sang thư mục của mình rồi sửa.

| MS | URL | View | FE | BE | Model | Form gửi lên |
|---|---|---|---|---|---|---|
| M3 | `GET /admin/trips…` | `admin/trip/list`, `…/form` | Trí | Trí | `trips` / `trip`, `routes`, `waterbuses` | `routeId`, `waterbusId`, `departureTime`, `status` |
| M3 | `GET /admin/waterbuses…` | `admin/waterbus/list`, `…/form` | Trí | Trí | `waterbuses` / `waterbus` | `code`, `name`, `capacity`, `status` |
| M3 | `GET /admin/stations`, `/new`, `/{id}/edit` | `admin/station/list`, `…/form` | Trí | Khuyên | `stations`: List StationDTO / `station`: StationForm | POST `/admin/stations/save`: `id` (rỗng = thêm mới), `code`, `name`, `address`, `isActive`; POST `/{id}/delete` chỉ ẩn bến |
| M3 | `GET /admin/routes`, `/new`, `/{id}/edit` | `admin/route/list`, `…/form` | Trí | Khuyên | `routes`: List RouteDTO / `route`: RouteForm (`stationIds` theo thứ tự đã lưu), `stations`: List StationDTO gồm cả bến inactive | POST `/admin/routes/save`: `id`, `code`, `name`, `description`, `isActive`, `stationIds` (lặp theo thứ tự); POST `/{id}/delete` chỉ ngừng hoạt động tuyến |
| M3 | `GET /admin/promotions…` | `admin/promotion/list`, `…/form` | Tân | Tân | `promotions` / `promotion` | `id`, `code`, `name`, `discountType`, `discountValue`, `maxDiscount`, `minOrderAmount`, `startAt`, `endAt`, `usageLimit`, `isActive` |
| M3 | `GET /admin/ticket-types…` | `admin/tickettype/list`, `…/form` | Tân | Tân | `ticketTypes` / `ticketType` | `code`, `name`, `price`, `requiresVerification`, `description`, `isActive` |

Thông báo sau khi lưu/xoá: BE dùng `RedirectAttributes.addFlashAttribute("successMessage", "...")`,
FE hiển thị `successMessage` / `errorMessage` qua fragment `alerts` trong `layout.html` (Khuyên làm ở M1).

### Quy tắc BE1 M3–M4

- Hai form admin bind request DTO riêng, không bind entity; `_isActive=on` đánh dấu checkbox,
  thiếu `isActive` khi bỏ chọn được bind thành false. ID sửa/ẩn không tồn tại không tạo bản ghi mới;
  GET form sửa trả 404, POST trả lỗi form/flash. GET không được ghi dữ liệu.
- Code bắt buộc, tối đa 20; tên bắt buộc, tối đa 100; địa chỉ/mô tả tối đa 255.
  Code được bỏ khoảng trắng đầu/cuối, kiểm tra trùng không phân biệt hoa/thường; constraint database vẫn là lớp kiểm tra cuối.
  Lỗi trả lại model đã nhập, `BindingResult` và `errorMessage` tiếng Việt. Template hiện chỉ hiển thị alerts chung.
- Tuyến cần ít nhất hai ID bến tồn tại, không trùng. Tuyến active không được lưu với bến inactive;
  tuyến inactive được giữ bến inactive. Form lấy cả bến inactive để không mất lựa chọn/dữ liệu cũ khi sửa.
  Ẩn bến không tự sửa tuyến/chuyến/vé: tuyến active cũ có bến vừa ẩn được giữ nguyên; khi lưu lại phải
  chọn bến active hoặc ngừng hoạt động tuyến. `/routes` vẫn liệt kê các tuyến active và đầy đủ bến theo thứ tự.
- Lưu tuyến/thứ tự bến trong một transaction; `stopOrder` bắt đầu 1, liên tục theo request.
  Sửa giữ ID và `minutesFromStart` của membership còn lại; bến mới dùng mặc định 0 đã có trong entity/schema.
  Đây là giá trị chưa cấu hình giờ, không phải thời gian di chuyển được tính; không thay đổi TripService hoặc chuyến hiện có.
  Dịch thứ tự tạm lên vùng số dương chưa dùng trước khi lưu thứ tự cuối để giữ unique `(route_id, stop_order)`.
  Chỉ xóa membership bỏ khỏi tuyến vì schema không có FK tới route_station; không xóa route/station hoặc cascade.
- **Cần Trí:** `templates/admin/station/form.html`, `templates/admin/route/form.html` chưa có lỗi dưới từng field:
  thêm `th:object` và `th:errors` tương ứng với BindingResult `station`/`route`, giữ tên field/action.
  `templates/admin/route/list.html` cần đổi nhãn nút/xác nhận “Xoá” sang “Ngừng hoạt động” cho đúng hành vi;
  URL POST `/{id}/delete` vẫn giữ nguyên. Các template admin được giữ nguyên trong phần BE1 này.
- **Cần Tân #33:** endpoint admin chưa được xác minh bảo vệ truy cập, cần Tân tích hợp #33.
  Không coi link ẩn trên navbar hay session trong test là phân quyền. Chưa sẵn sàng phát hành admin.
- 404/500 dùng error dispatch của Spring Boot và template `error/404`, `error/500` hiện có;
  endpoint gây lỗi 500 chỉ nằm trong test. Không có catch-all controller hoặc global advice mới.
