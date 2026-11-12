# Chuẩn bị môi trường (mỗi thành viên làm 1 lần)

Mất khoảng 30-45 phút. Làm theo thứ tự, xong bước nào tích bước đó.

## 1. Cài đặt

| Phần mềm | Phiên bản | Ghi chú |
|---|---|---|
| JDK | **21** | Temurin 21 hoặc để IntelliJ tự tải (bước 4) |
| IntelliJ IDEA | Community hoặc Ultimate (sinh viên được Ultimate miễn phí) | |
| Git | bản mới nhất | Windows: Git for Windows (có sẵn Git Bash) |
| SQL Server | 2019/2022 Developer hoặc Express | |
| SSMS | bản mới nhất | để chạy script tạo database |

**Không cần cài Maven** - project có sẵn Maven Wrapper (`mvnw` / `mvnw.cmd`) giống project mẫu.

## 2. Cấu hình SQL Server (hay lỗi nhất)

1. **Bật đăng nhập bằng user/mật khẩu**: SSMS → chuột phải tên server → *Properties → Security* →
   chọn **SQL Server and Windows Authentication mode** → OK.
2. **Bật user `sa`**: *Security → Logins → sa* → *Properties* → đặt mật khẩu; tab *Status* → *Login: Enabled*.
3. **Bật TCP/IP port 1433** (JDBC bắt buộc): mở **SQL Server Configuration Manager** →
   *SQL Server Network Configuration → Protocols for MSSQLSERVER* (hoặc SQLEXPRESS) → **TCP/IP: Enabled** →
   chuột phải TCP/IP → *Properties → IP Addresses* → kéo xuống **IPAll**: xoá *TCP Dynamic Ports*,
   *TCP Port* = `1433`.
4. **Restart service**: *SQL Server Services* → chuột phải *SQL Server (...)* → **Restart**.

## 3. Tạo database

Mở SSMS → *File → Open* → `database/WaterbusDB_SQLServer.sql` → **Execute** (F5).
Script KH3 tạo database `WaterbusDB`, 14 bảng và dữ liệu mẫu (8 bến, 6 tuyến, chuyến 7 ngày tới,
4 tài khoản, 3 mã khuyến mãi). Đã gộp ba migration lịch sử; máy mới chỉ chạy một file.
Chạy lại sẽ xoá bảng và dữ liệu cũ rồi tạo lại, kể cả bảng hoàn tiền và nhật ký soát vé.
Lịch phân công tàu còn chờ TI7, chỉ dùng làm demo khi kiểm tra chồng giờ trả kết quả rỗng.

Tài khoản mẫu: `admin@waterbus.vn / Admin@123`, `staff@waterbus.vn / Staff@123`, `customer@gmail.com / Customer@123`.

Super admin (id = 0): `superadmin@waterbus.vn / SuperAdmin@123`. Không hiện trong danh sách tài khoản ở trang admin,
không bị đổi quyền, khoá hay reset mật khẩu. DB tạo trước ngày 12/11/2026 thì chạy thêm
`database/changes/2026-11-12_leader_super-admin.sql`.

Mã khuyến mãi mẫu: `WELCOME10` (giảm 10%, tối đa 10.000đ), `GIAM5K` (giảm 5.000đ cho đơn từ 30.000đ),
`FREE100` (giảm 100%, dùng tối đa 5 lần, hạn 1 tháng).

**Kịch bản demo đơn 0 đ:** đặt vé bất kỳ, nhập mã `FREE100` ở bước đặt vé → tổng tiền 0 đ → đơn chuyển
thẳng sang **PAID** (vé VALID), **không** chuyển sang trang VNPay. Mỗi lần demo tốn 1 lượt; hết 5 lượt thì
reset `used_count` (`UPDATE promotion SET used_count = 0 WHERE code = 'FREE100'`) hoặc chạy lại script.

DB đang sử dụng cần CASH và tên WELCOME10 mới: chạy `database/changes/2026-11-13_db_tien-mat-ten-khuyen-mai.sql`.
Xem `database/changes/README.md` để phân biệt nâng cấp DB cũ với khởi tạo mới; không chạy lại migration lịch sử trên DB mới.

## 4. Mở project bằng IntelliJ

1. *File → Open* → chọn thư mục `waterbus` (thư mục có `pom.xml`) → *Trust Project*.
2. *File → Project Structure → Project → SDK* → chọn **21** (chưa có thì *Add SDK → Download JDK → 21, Eclipse Temurin*).
3. Đợi IntelliJ tải thư viện Maven (thanh tiến trình góc dưới). Nếu không tự tải: mở tab **Maven**
   bên phải → nút **Reload All Maven Projects**.
4. Kiểm tra *Settings → Editor → Code Style* → **Enable EditorConfig support** đang bật (mặc định có).

## 5. Cấu hình riêng của máy bạn

Copy file `src/main/resources/application-local.properties.example` thành
`src/main/resources/application-local.properties` (cùng thư mục) rồi sửa mật khẩu `sa` của bạn:

```properties
spring.datasource.username=sa
spring.datasource.password=mat_khau_sa_cua_ban
```

File này **đã nằm trong `.gitignore`** → mật khẩu của bạn không bao giờ lên GitHub, và 4 người
dùng 4 mật khẩu khác nhau mà không ai phải sửa `application.properties` (file chung).
Dùng instance tên (SQLEXPRESS) hoặc port khác → xem các dòng mẫu trong file `.example`.

## 6. Chạy thử

- Mở `src/main/java/com/hsf302/waterbus/WaterbusApplication.java` → bấm ▶ cạnh `main`.
- Vào http://localhost:8080 → thấy trang chủ.
- Vào http://localhost:8080/stations → thấy **5 bến** từ database = kết nối DB thành công.
- Các trang khác (Tìm chuyến, Đặt vé...) đang là trang mẫu, chờ từng module làm.

Chạy test (không cần SQL Server, dùng H2 trong RAM):
```bash
./mvnw test          # Windows CMD/PowerShell: mvnw.cmd test
```

## 7. Bật hook Git (bắt buộc)

```bash
git config core.hooksPath .githooks
```
Thử: `git commit --allow-empty -m "test Co-Authored-By: Copilot"` → phải bị **chặn**.

## Lỗi thường gặp

| Lỗi | Cách sửa |
|---|---|
| `The TCP/IP connection to the host localhost, port 1433 has failed` | Chưa bật TCP/IP / chưa restart service (bước 2.3-2.4) |
| `Login failed for user 'sa'` | Sai mật khẩu trong `application-local.properties`, hoặc chưa bật Mixed mode / `sa` (bước 2.1-2.2) |
| `Cannot open database "WaterbusDB"` | Chưa chạy script (bước 3) |
| `Invalid object name 'xxx'` / `Invalid column name` | DB của bạn cũ hơn code → chạy lại script + các file trong `database/changes/` |
| `Port 8080 was already in use` | Tắt app đang chạy, hoặc thêm `server.port=8081` vào file local |
| Chữ tiếng Việt bị `???` | Cột phải là `NVARCHAR` và entity có `@Nationalized` |
| IntelliJ báo đỏ hết import | Chưa chọn SDK 21 hoặc chưa Reload Maven (bước 4) |
| `Parameter 0 of constructor ... required a bean ... that could not be found` | ServiceImpl thiếu `@Service` |
