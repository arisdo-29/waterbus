# AGENTS.md - Hướng dẫn cho AI coding agent

File này dành cho mọi AI agent làm việc trong repo (Claude Code đọc qua `CLAUDE.md`,
các agent khác đọc trực tiếp `AGENTS.md`). Thành viên nhóm cũng nên đọc để biết agent được phép làm gì.

---

## 0. LUẬT CỨNG VỀ GIT - KHÔNG CÓ NGOẠI LỆ

> **EN (hard rule):** You MAY create commits, but a commit must NEVER contain the name of
> any AI agent or tool. No `Co-Authored-By:` trailer for an AI, no "Generated with ..."
> line, no session links, no robot emoji, no agent/tool name anywhere in the subject or
> body. The same applies to PR titles and descriptions. Never change `git config user.name`
> / `user.email`: commits are authored by the human developer only.

1. Agent **được phép tự commit**, nhưng commit **tuyệt đối không được kèm tên của agent**:
   - Không thêm dòng `Co-Authored-By: <tên AI> ...`.
   - Không thêm dòng `Generated with ...`, link session, biểu tượng robot.
   - Không nhắc tên agent/công cụ AI ở tiêu đề hay nội dung commit.
   - Tiêu đề và mô tả Pull Request cũng theo đúng quy định này.
2. **Không sửa** `git config user.name` / `user.email`. Tác giả commit luôn là thành viên đang dùng agent.
3. **Không push** thẳng lên `main` hoặc `develop`. Chỉ commit/push trên nhánh `feature/...` của người dùng.
4. **Không** `git push --force`, `git reset --hard` lên nhánh đã push, không xoá nhánh của người khác.
5. **Không** dùng `--no-verify` để né hook `.githooks/commit-msg`. Nếu hook chặn, sửa message cho đúng.
6. Message theo Conventional Commits, viết ngắn gọn (tiếng Việt không dấu hoặc tiếng Anh đều được):
   ```
   feat(trip): tim chuyen theo ben di, ben den va ngay
   fix(booking): tinh lai tong tien khi bo ma khuyen mai
   ```
   Kiểu hợp lệ: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`.
   Scope = tên module: `station`, `route`, `trip`, `waterbus`, `auth`, `promotion`, `tickettype`, `booking`, `payment`, `shared`, `db`.

Cơ chế kiểm tra: hook `.githooks/commit-msg` (local) và job `no-agent-name` trên GitHub Actions
(mọi PR). Vi phạm thì commit/PR bị chặn.

---

## 1. Dự án

- **Saigon Waterbus** - web đặt vé buýt đường sông, môn HSF302. Phạm vi: luồng chính đặt vé,
  thanh toán (MOCK), khuyến mãi. Không làm AI, GPS.
- Stack: **Java 21, Spring Boot 4.1.1, Spring MVC + Thymeleaf, Spring Data JPA, SQL Server**, Maven Wrapper.
- IDE chính: IntelliJ IDEA. Không dùng Lombok (giữ giống project mẫu của giảng viên).

## 2. Cấu trúc (giống project mẫu của giảng viên)

```
src/main/java/com/hsf302/waterbus/
├── WaterbusApplication.java
├── controller/      @Controller, mỗi module 1 controller, có @RequestMapping("/<prefix>") riêng
├── dto/             Dữ liệu đưa ra view (kiểu StationDTO / ArtifactDTO)
├── entity/          @Entity khớp 1-1 với database/WaterbusDB_SQLServer.sql
├── repository/      interface extends JpaRepository<Entity, Integer>
└── service/         interface
    └── impl/        class implements interface, PHẢI có @Service
src/main/resources/
├── templates/<module>/   view Thymeleaf của từng module
├── templates/fragments/layout.html   head/navbar/scripts dùng chung
└── static/css, static/js
database/                 script SQL Server (nguồn sự thật của schema)
```

Luồng chuẩn (xem ví dụ đầy đủ ở module Station):
`Controller -> Service (interface) -> ServiceImpl (@Service) -> Repository -> Entity`, rồi map Entity sang DTO trả cho view.

## 3. Chia module và quyền sửa file

| Module | Phụ trách | Entity / Service / Controller | URL prefix | Thư mục view |
|---|---|---|---|---|
| BE1 Tuyến & Bến | BE1 | Station, Route, RouteStation | `/stations`, `/routes`, `/admin/stations`, `/admin/routes` | `station/`, `route/` |
| BE2 Tàu, Ghế & Chuyến | BE2 | Waterbus, Seat, Trip | `/trips`, `/admin/waterbuses`, `/admin/trips` | `trip/`, `waterbus/` |
| BE3 Tài khoản, Loại vé & KM | BE3 | Account, TicketType, Promotion | `/auth`, `/account`, `/admin/ticket-types`, `/admin/promotions` | `auth/`, `tickettype/`, `promotion/` |
| BE4 Đặt vé & Thanh toán | BE4 | Booking, Ticket, Payment | `/booking`, `/payment`, `/my-bookings` | `booking/`, `payment/` |

Quy tắc cho agent:
- Chỉ sửa file thuộc module của người đang dùng agent. Cần sửa file module khác -> **dừng lại, hỏi người dùng**.
- **File chung** (chỉ sửa khi người dùng yêu cầu rõ ràng): `pom.xml`, `application.properties`,
  `templates/fragments/layout.html`, `HomeController`, `entity/*`, `database/WaterbusDB_SQLServer.sql`,
  `.github/*`, `.githooks/*`, `AGENTS.md`, `CLAUDE.md`.
- Method trong service có ghi chú "HỢP ĐỒNG" là module khác đang gọi: không đổi tên/tham số/kiểu trả về.
  Được thêm method mới.
- Gọi sang module khác qua **Service interface**, không gọi thẳng Repository của module khác.

## 4. Quy ước code

- Inject bằng constructor (không `@Autowired` field).
- `ServiceImpl` luôn có `@Service`; method ghi dữ liệu nhiều bảng dùng `@Transactional`.
- Không trả Entity thẳng ra view nếu có quan hệ LAZY phức tạp; tạo DTO trong `dto/`.
- Tên bảng/cột lấy đúng theo script SQL. Trạng thái là chuỗi viết HOA đúng như CHECK constraint
  (VD: `PENDING`, `PAID`, `HOLD`, `VALID`).
- Tiền: `BigDecimal`. Thời gian: `LocalDateTime`.
- Mật khẩu: `BCryptPasswordEncoder` (đã có `spring-security-crypto`), không lưu mật khẩu thô.
- View: mọi trang dùng `th:replace="~{fragments/layout :: head('Tiêu đề')}"`, `navbar`, `scripts`.
- Không format lại toàn bộ file có sẵn (gây conflict); chỉ format phần mình sửa.

## 5. Database

- `spring.jpa.hibernate.ddl-auto=none`: schema chỉ đến từ `database/WaterbusDB_SQLServer.sql`.
- Cần đổi bảng/cột: tạo file mới `database/changes/YYYY-MM-DD_<module>_<mo-ta>.sql`
  + sửa Entity tương ứng, trong cùng 1 PR. Không sửa trực tiếp file script chính.
- Không ghi mật khẩu vào `application.properties`; cấu hình riêng nằm trong
  `application-local.properties` (bị gitignore).

## 6. Kiểm tra trước khi commit

```bash
./mvnw -q test          # Windows: mvnw.cmd -q test
```
Test `contextLoads` khởi động app bằng H2 trong RAM: entity map sai, thiếu `@Service`,
trùng URL giữa 2 controller... đều làm test fail. Test phải pass trước khi commit.
