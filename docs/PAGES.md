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
| M1 | `POST /booking` | redirect `/payment/{bookingCode}` | - | Trâm | lỗi validate → render lại `booking/checkout` với `errorMessage`, đầy đủ model checkout và request parameters đã nhập. M4: form bind vào **`bookingForm`** (`BookingForm`) + BindingResult; lỗi từng ô ở field `contactName`, `contactPhone`, `contactEmail`, `promoCode`, `seatIds` (xem ghi chú validate bên dưới) | |
| M1 | `GET /payment/{bookingCode}` | `payment/index` | Khuyên | Trâm | `booking` (`bookingCode`, `status`, `subtotal`, `discountAmount`, `totalAmount`, `expiresAt`), `trip`, `tickets` | form → `POST /payment/{bookingCode}/vnpay` (chỉ còn VNPay; thanh toán MOCK `/payment/{bookingCode}/mock` đã bỏ) |
| M5 | `POST /payment/{bookingCode}/vnpay` | redirect sang cổng VNPay | - | Trâm | Đơn không còn `PENDING`/quá hạn → redirect `/payment/{bookingCode}` + flash `errorMessage` | `bankCode` (không bắt buộc, đặt trên URL action, VD `/payment/{bookingCode}/vnpay?bankCode=VNPAYQR`): `VNPAYQR` = mở thẳng màn hình QR, `VNBANK` = mở thẳng trang thẻ ATM nội địa, bỏ trống (hoặc mã khác) = trang chọn phương thức của VNPay |
| M5 | `GET /payment/vnpay-return` | redirect | - | Trâm | Thành công → `/booking/{bookingCode}` + flash `successMessage`; thất bại/huỷ → `/payment/{bookingCode}` + flash `errorMessage` | tham số `vnp_*` do VNPay gửi về |
| M1 | `GET /booking/{bookingCode}` | `booking/detail` | Khuyên | Trâm | `booking` (`bookingCode`, `status`, `contactName`, `contactPhone`, `contactEmail`, `totalAmount`, `createdAt`, `paidAt`), `trip`, `tickets` (mỗi vé: `ticketCode`, `seatCode`, `fromStation`, `toStation`, `price`, `status`) | |
| TR2 | `GET /booking/{bookingCode}/verify` | `booking/verify` (**chưa có - KH5 Khuyên**) | Khuyên | Trâm | `bookingCode`; lỗi nhập sai SĐT qua flash `errorMessage` (fragment alerts) | `<form method="post">` tới `/booking/{bookingCode}/verify`, field **`contactPhone`** |
| TR2 | `POST /booking/{bookingCode}/verify` | redirect | - | Trâm | Đúng SĐT liên hệ của đơn khách vãng lai → `/booking/{bookingCode}` (trình duyệt này được xem đơn); sai → `/booking/{bookingCode}/verify` + flash `errorMessage` = "Số điện thoại không khớp với đơn đặt vé này." | |
| M4 | `POST /booking/{bookingCode}/cancel` | redirect `/booking/{bookingCode}` | Khuyên (nút "Huỷ đơn" trên `booking/detail`) | Trâm | flash `successMessage` = "Đã huỷ đơn." hoặc `errorMessage` (không tìm thấy đơn / đơn không còn `PENDING` / không phải đơn của tài khoản đang đăng nhập) | Nút **chỉ hiện khi `booking.status == 'PENDING'`**; `<form method="post">` tới `/booking/{bookingCode}/cancel`, không cần field |
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

**Validate checkout (M4, BE4 → Khuyên):** model của `booking/checkout` **luôn có `bookingForm`** (lần đầu vào là object rỗng,
khi `POST /booking` lỗi thì kèm BindingResult). Tên field giữ nguyên như cột "Form gửi lên". Để hiện lỗi dưới từng ô,
thêm `th:object="${bookingForm}"` vào thẻ `<form>` và dưới mỗi ô, ví dụ ô họ tên:
`<input ... th:classappend="${#fields.hasErrors('contactName')} ? 'is-invalid'">`
`<div class="invalid-feedback" th:if="${#fields.hasErrors('contactName')}" th:errors="*{contactName}"></div>`
(tương tự `contactPhone`, `contactEmail`, `promoCode`). **Giữ `th:value` như hiện tại**, không đổi sang `th:field`, để vẫn
ưu tiên giá trị vừa nhập và tự điền từ `session.loginAccount` (#26). Lỗi `seatIds` (ô ẩn) chỉ hiện qua `errorMessage`.

**Vé ưu đãi, chính sách, hoàn tiền (đã có FE/BE, các sửa lỗi TR theo phân công 12/11 vẫn đang chờ):**

- Quy tắc: loại vé `requiresVerification = true` (`SENIOR_70`, `PRIORITY`, giá 0 đ) cần họ tên + năm sinh cho **mỗi ghế**.
  Năm sinh 1900..năm hiện tại; `SENIOR_70`: năm đi − năm sinh ≥ 70; `PRIORITY`: không giới hạn tuổi (nhóm không có vé trẻ em).
  Vé ưu đãi phải đăng nhập và tối đa 2 vé mỗi đơn (BookingService.createBooking báo lỗi lên trang checkout). Không tải ảnh giấy tờ,
  nhân viên kiểm tra giấy tờ gốc tại bến. Đơn tổng 0 đ được xác nhận ngay (redirect thẳng `/booking/{bookingCode}`, không qua `/payment`).
  Vé đã thanh toán không đổi, không hoàn; chỉ khi nhà vận hành huỷ chuyến thì đơn đã trả tiền "Chờ hoàn tiền", admin hoàn thủ công → `REFUNDED`.
- `POST /booking` gửi thêm: `passengerNames` (lặp, đúng thứ tự `seatIds`, chỉ có khi vé ưu đãi), `birthYears` (lặp, cùng thứ tự),
  `acceptPolicy` (checkbox `value="true"`, bắt buộc). Lỗi gắn vào field `passengerNames[i]` / `birthYears[i]` hiện dưới ô của ghế thứ i;
  lỗi gắn vào `passengerNames` / `birthYears` (cả danh sách) hiện cuối khung hành khách; lỗi `acceptPolicy` hiện dưới ô tick.
  Template giữ giá trị đã nhập từ request parameters. `ticketType` của checkout cần có `code`, `requiresVerification`.
- `booking/detail` thêm model: `refund` (có thể null: `status` PENDING/DONE, `amount`, `reason`, `transactionRef`, `refundedAt`),
  `tripCancelReason` (có thể null), `payment` (có thể null: `method` VNPAY, hoặc MOCK/MOMO của dữ liệu cũ, `transactionRef`);
  mỗi phần tử `tickets` thêm `passengerName`, `birthYear` (null nếu vé thường), nên có thêm `requiresVerification` (nhãn "Mang giấy tờ gốc";
  nếu thiếu, FE coi vé có `birthYear` là vé ưu đãi). Booking `REFUNDED` không có `refund` vẫn hiện banner "Đã hoàn tiền" với `totalAmount`.
- `booking/history`: mỗi phần tử `bookings` thêm `refundStatus` (null / `PENDING` / `DONE`).
- **Trạng thái vé hiển thị (TR5):** `tickets[].status` có thêm mã **`PASSED`** = vé `VALID` của chuyến đã `COMPLETED`
  (chỉ đổi chữ hiển thị, DB vẫn `VALID`). **Cần Khuyên** thêm nhãn "Đã qua chuyến" cho `PASSED` trong fragment `statusBadge`;
  chưa thêm thì badge in nguyên chữ `PASSED`. Chuyến `DEPARTED` vẫn đặt vé và thanh toán được cho khách lên ở bến giữa (tới mốc ngừng bán).
- **Quyền xem đơn (TR2):** `GET /booking/{bookingCode}`, `POST /booking/{bookingCode}/cancel`, `GET /payment/{bookingCode}`,
  `POST /payment/{bookingCode}/vnpay` chỉ cho chủ đơn, STAFF/ADMIN, hoặc trình duyệt đã được phép (session `viewableBookingCodes`:
  vừa đặt đơn khách vãng lai, hoặc đã nhập đúng SĐT ở `/booking/{bookingCode}/verify`). Đơn của tài khoản mà chưa đăng nhập →
  `/auth/login` (đăng nhập xong quay lại đúng trang); tài khoản khác → `/` + flash "Bạn không có quyền xem đơn đặt vé này.";
  đơn khách vãng lai mở trên trình duyệt khác → `/booking/{bookingCode}/verify`. Luật nằm ở `BookingService.checkViewAccess`.
- Field mới trên DTO đã có (`tickets[]`, `bookings[]`) được template đọc qua `${@view.prop(obj, 'tenField')}` (bean `ViewPropertyHelper`):
  DTO chưa có field thì coi như null, không lỗi 500. Thêm field vào DTO là giao diện tự hiện.

**Trạng thái tích hợp:** các endpoint khách hàng ở bảng trên (tìm chuyến, chọn ghế, checkout, thanh toán, xem vé, huỷ đơn,
lịch sử đặt vé) đã được triển khai và nối với service/database. `GET /booking` chuyển hướng sang `/my-bookings`.

## Trang quản trị (M3)

Trí làm mẫu chung `templates/admin/_template/list.html` (bảng + nút Thêm) và `form.html` (thêm/sửa)
đầu tiên trong M3; mỗi chủ trang copy mẫu sang thư mục của mình rồi sửa.

| MS | URL | View | FE | BE | Model | Form gửi lên |
|---|---|---|---|---|---|---|
| M3 | `GET /admin/trips…` | `admin/trip/list`, `…/form` | Trí | Trí | `trips` / `trip`, `routes`, `waterbuses` | `routeId`, `waterbusId`, `departureTime`, `status` |
| M3 | `GET /admin/waterbuses`, `/new` | `admin/waterbus/list`, `…/form` | Trí | Trí | `waterbuses`, `scheduledTripCounts` (theo ID tàu) / `waterbus` | POST `/admin/waterbuses/save`: `code`, `name`, `capacity`, `status` (chỉ thêm mới); POST `/{id}/status`: `status`; không có trang sửa tàu |
| M3 | `GET /admin/stations`, `/new`, `/{id}/edit` | `admin/station/list`, `…/form` | Trí | Khuyên | `stations`: List StationDTO / `station`: StationForm | POST `/admin/stations/save`: `id` (rỗng = thêm mới), `code`, `name`, `address`, `isActive`; POST `/{id}/delete` chỉ ẩn bến |
| M3 | `GET /admin/routes`, `/new`, `/{id}/edit` | `admin/route/list`, `…/form` | Trí | Khuyên | `routes`: List RouteDTO / `route`: RouteForm (`stationIds`, `minutesFromStart` cùng thứ tự đã lưu), `stations`: List StationDTO gồm cả bến inactive | POST `/admin/routes/save`: `id`, `code`, `name`, `description`, `isActive`, `stationIds`, `minutesFromStart` (hai danh sách lặp cùng thứ tự, xem hợp đồng KH1 bên dưới); POST `/{id}/delete` chỉ ngừng hoạt động tuyến |
| M3 | `GET /admin/promotions…` | `admin/promotion/list`, `…/form` | Tân | Tân | `promotions` / `promotion` | `id`, `code`, `name`, `discountType`, `discountValue`, `maxDiscount`, `minOrderAmount`, `startAt`, `endAt`, `usageLimit`, `isActive` |
| M3 | `GET /admin/ticket-types…` | `admin/tickettype/list`, `…/form` | Tân | Tân | `ticketTypes` / `ticketType` | `code`, `name`, `price`, `requiresVerification`, `description`, `isActive` |
| M5 | `GET /admin/revenue?from=yyyy-MM-dd&to=yyyy-MM-dd` (chỉ ADMIN, `AdminInterceptor` đã chặn STAFF) | `admin/revenue/index` | Trí | Trí | `from`, `to`: LocalDate đã áp dụng mặc định (không truyền → 7 ngày gần nhất: `to` = hôm nay, `from` = hôm nay − 6 ngày; `from` > `to` → `errorMessage` và dùng lại mặc định); `errorMessage` (có thể null); `summary`: RevenueSummaryDTO (`totalRevenue`, `paidBookings`, `ticketsSold`, `freeTickets`, `totalDiscount`, `totalRefunded`, `cancelledOrExpiredBookings`); `dailyRevenue`: List DailyRevenueDTO (`date`, `revenue`, `bookings`; đủ mọi ngày trong khoảng, ngày không có đơn = 0 để vẽ biểu đồ); `byRoute`, `byTicketType`, `byMethod`, `byPromotion`: List RevenueGroupDTO (`name`, `bookings`, `tickets`, `revenue`; sắp xếp `revenue` giảm dần). Cách tính: cộng payment `SUCCESS` theo `Payment.paidAt`, trừ refund `DONE` theo `Refund.refundedAt`; `summary.totalRevenue` bằng tổng `dailyRevenue.revenue`. Vé miễn phí (đơn 0 đ, không có payment) đếm vào `freeTickets` theo `Booking.createdAt` | Form GET `/admin/revenue`: `from`, `to` (`input type="date"`) |
| M5 | `GET /admin/refunds?status=` | `admin/refund/list` | Trí | Trí | `refunds`: List RefundAdminDTO (`id`, `bookingCode`, `amount`, `status`, `reason`, `contactName`, `contactPhone`, `contactEmail`, `transactionRef`, `refundedAt`); `statusFilter`: `PENDING`, `DONE` hoặc rỗng | GET `status` (rỗng = tất cả); POST `/admin/refunds/{id}/complete` (nút "Xác nhận đã hoàn tiền", chỉ nhận `id` từ URL; hoàn tiền thật xử lý ngoài hệ thống) |
| Mới | `GET /admin/accounts…` | `admin/account/list`, `…/form`, `…/detail` | Trí | Tân | chưa chốt | chưa chốt |
| M5 | `GET /admin/checkin` | `admin/checkin/index` | Trí | Tân | xem mục "Soát vé – hợp đồng JS" | xem mục "Soát vé – hợp đồng JS" |
| M2 | `GET/POST /account/profile` | `account/profile` | Trí | Tân | `account` (AccountDTO), `updateProfileRequest` | `fullName`, `phone` |
| M2 | `GET/POST /account/change-password` | `account/change-password` | Trí | Tân | `changePasswordRequest`, `errorMessage` | `currentPassword`, `newPassword`, `confirmPassword` |

Menu quản trị (`adminSidebar` trong `layout.html`): ADMIN thấy Chuyến tàu, Doanh thu, Hoàn tiền, Tài khoản, Tàu, Bến, Tuyến,
Khuyến mãi, Loại vé, Soát vé; STAFF thấy Soát vé (đầu danh sách) và Chuyến tàu. Dropdown tài khoản trên navbar của ADMIN
có thêm "Doanh thu". Ẩn link chỉ là giao diện; BE của từng trang vẫn phải tự chặn quyền (chỉ ADMIN cho 3 URL mới).
Mục đang mở được tô sáng theo tiền tố URL (VD `/admin/accounts/5/edit` sáng "Tài khoản").

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
  Không được ẩn bến còn thuộc tuyến active, kể cả bỏ chọn `isActive` trên form sửa; phải ngừng tuyến trước.
  Bến chỉ thuộc tuyến inactive được phép ẩn; không tự xoá tuyến/chuyến/vé. `/routes` vẫn liệt kê tuyến active theo thứ tự.
  KH2 phần tuyến còn chờ TI3: kiểm tra số chuyến trước khi ngừng tuyến/đổi hành trình; chưa coi D5 hoàn tất.
- Lưu tuyến/thứ tự bến trong một transaction; `stopOrder` bắt đầu 1, liên tục theo request.
  Sửa giữ ID của membership còn lại; `minutesFromStart` lấy từ form, cùng thứ tự `stationIds`.
  Mỗi bến có một số phút nguyên không âm; bến đầu = 0, các bến sau tăng nghiêm ngặt. Không tự điền 0 khi thiếu dữ liệu.
  KH1 không tự tính lại giờ của chuyến đã tạo; KH2 sẽ chặn thay hành trình khi còn chuyến sắp chạy (chờ TI3).
  Dịch thứ tự tạm lên vùng số dương chưa dùng trước khi lưu thứ tự cuối để giữ unique `(route_id, stop_order)`.
  Chỉ xóa membership bỏ khỏi tuyến vì schema không có FK tới route_station; không xóa route/station hoặc cascade.
- Trí đã thêm `th:object`/`th:errors` cho `admin/station/form.html`, `admin/route/form.html` và đổi nút tuyến sang
  “Ngừng hoạt động” (URL POST `/{id}/delete` giữ nguyên) - kiểm tra lại 08/11/2026.
- **Cần Tân #33:** endpoint admin chưa được xác minh bảo vệ truy cập, cần Tân tích hợp #33.
  Không coi link ẩn trên navbar hay session trong test là phân quyền. Chưa sẵn sàng phát hành admin.
- 404/500 dùng error dispatch của Spring Boot và template `error/404`, `error/500` hiện có;
  endpoint gây lỗi 500 chỉ nằm trong test. Không có catch-all controller hoặc global advice mới.

## Hợp đồng phút qua bến (KH1, chờ Trí tích hợp TI4)

- Form `route` có `stationIds` và `minutesFromStart`: hai danh sách cùng số phần tử, cùng thứ tự trên trang.
  Ví dụ bến `[A, B, C]` đi cùng phút `[0, 15, 35]`. Khi sửa, backend tải cả hai danh sách theo `stopOrder`.
- TI4 thêm `<input type="number" name="minutesFromStart" min="0" step="1" required>` trong **mỗi**
  `[data-route-row]`, gồm cả hàng mặc định và `[data-route-row-template]`; lấy giá trị theo chỉ số của hàng.
  Khi chuyển hàng lên/xuống, chuyển nguyên hàng gồm bến và phút. Không tự sửa phút sau khi đảo bến.
- Hiện lỗi `minutesFromStart` và `minutesFromStart[i]` dưới khung/ô tương ứng bằng `th:errors`/`#fields.errors`.
  Lỗi khác số phần tử, bến đầu khác 0 hoặc phút không tăng được backend báo qua `errorMessage`.
- `app.js` kiểm tra ô phút khi có trong template; backend vẫn kiểm tra lại khi không chạy JavaScript.
  Không có giá trị mặc định thay cho dữ liệu phút bị thiếu. Chỉ tích hợp KH1 vào `develop` cùng TI4 đã kiểm tra.

## Soát vé – hợp đồng JS

Template `admin/checkin/index.html` do **Trí** giữ, backend do **Tân** làm; JS (`initCheckin` trong `app.js`), CSS và thư viện
`static/js/html5-qrcode.min.js` (v2.3.8, Apache-2.0) do **Khuyên** làm. Mục tiêu: mỗi khách 2 thao tác (quét → xác nhận).

| Data-attribute | Gắn vào | Ý nghĩa |
|---|---|---|
| `data-checkin-form` | `<form method="get" th:action="@{/admin/checkin}">` tra mã | JS chặn gửi trùng: cùng mã trong 3 giây chỉ gửi 1 lần |
| `data-checkin-input` | `<input name="ticketCode">` trong form trên (mã vé **hoặc** mã đơn) | Tự focus khi tải trang và sau mỗi lượt; tự viết hoa, bỏ khoảng trắng; Enter là gửi (máy quét USB/Bluetooth dùng được ngay) |
| `data-qr-start` | `<button type="button">` "Quét QR" | Mở camera sau; đọc được mã thì điền vào ô, tắt camera, gửi form tra mã |
| `data-qr-reader` | `<div>` rỗng (khung camera) | JS tự ẩn/hiện; nút "Tắt camera" và dòng thông báo do JS tự thêm (hoặc tự đặt `data-qr-stop`, `data-qr-message`) |
| `data-checkin-preview` | Modal Bootstrap (`class="modal fade"`) server render khi `GET /admin/checkin?ticketCode=...` tìm thấy vé/đơn | JS tự mở; trong modal là `<form method="post" th:action="@{/admin/checkin}">` có hidden `ticketCode`; nút submit gắn **`data-checkin-confirm`** được focus sẵn (bấm 1 lần hoặc Enter). Đóng modal → ô nhập trống, focus lại |
| `data-checkin-result` | Thẻ rỗng server render sau khi soát (POST xong) | `data-result="success"` hoặc `"error"`, `data-message="Nội dung"` (VD "Vé TK0001 - ghế B3, mời lên tàu"). JS hiện khung to toàn màn hình xanh/đỏ, âm báo, rung, 2 giây sau tự ẩn |

Trang soát vé dùng fragment **`scriptsCheckin`** THAY cho `scripts`:
`<th:block th:replace="~{fragments/layout :: scriptsCheckin}"></th:block>`.
Camera chỉ chạy trên `http://localhost` hoặc HTTPS (điện thoại truy cập qua IP LAN sẽ được báo lỗi tiếng Việt, vẫn nhập tay/máy quét được).
Kiểm tra JS: `node src/test/js/checkin.browser.cjs` (tự sinh trang mẫu đúng hợp đồng trên).
