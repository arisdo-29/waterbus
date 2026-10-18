# Saigon Waterbus - HSF302

Web đặt vé buýt đường sông (tham khảo saigonwaterbus.com): tìm chuyến, chọn ghế, đặt vé,
thanh toán (MOCK), khuyến mãi.

**Stack:** Java 21 · Spring Boot 4.1.1 · Spring MVC + Thymeleaf · Spring Data JPA · SQL Server · Maven Wrapper

Cấu trúc thư mục giống project mẫu của giảng viên (`controller / dto / entity / repository / service / service.impl`
+ `templates` + `static`), có thêm phần chia module cho 4 người.

## Nhóm

| | Người | Phụ trách |
|---|---|---|
| BE1 | Khuyên | FE chính (layout, trang chủ, checkout, thanh toán, xem vé) + Tuyến & Bến |
| BE2 | Trí | Tàu, Ghế & Chuyến + UI tìm chuyến, sơ đồ chọn ghế, mẫu trang admin |
| BE3 | Tân | Tài khoản, Loại vé & Khuyến mãi + trang đăng nhập/đăng ký, admin khuyến mãi |
| BE4 | Trâm | Đặt vé & Thanh toán |

Việc chia theo milestone M1 → M4 trong [docs/MODULES.md](docs/MODULES.md).
**M1 - Demo luồng đặt vé: hạn Thứ 5 22/10/2026, 17:00.**

## Bắt đầu nhanh

```bash
git clone https://github.com/<tai-khoan>/waterbus.git
cd waterbus
git config core.hooksPath .githooks
# 1. Chạy database/WaterbusDB_SQLServer.sql trong SSMS
# 2. Copy src/main/resources/application-local.properties.example -> application-local.properties, sửa mật khẩu sa
# 3. Mở bằng IntelliJ, chạy WaterbusApplication -> http://localhost:8080/stations
```

## Tài liệu

| File | Nội dung |
|---|---|
| [docs/SETUP.md](docs/SETUP.md) | Cài JDK, SQL Server, IntelliJ, chạy app lần đầu, lỗi thường gặp |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Tạo repo GitHub, nhánh, Pull Request, luật chống conflict |
| [docs/MODULES.md](docs/MODULES.md) | Thành viên, ai sở hữu file nào, việc theo milestone M1 → M4 |
| [docs/PAGES.md](docs/PAGES.md) | Hợp đồng từng trang: URL, view, attribute, field form giữa FE và BE |
| [AGENTS.md](AGENTS.md) | Quy định cho AI agent (commit không kèm tên agent) |
| [database/](database/) | Script SQL Server + thư mục `changes/` cho thay đổi schema |

## Cấu trúc

```
waterbus/
├── .github/            CI (build + kiểm tra tên agent), PR template, CODEOWNERS
├── .githooks/          commit-msg: chặn commit có tên AI agent
├── .claude/            cấu hình chung cho Claude Code (tắt ghi công trong commit)
├── database/           WaterbusDB_SQLServer.sql, changes/
├── docs/
├── src/main/java/com/hsf302/waterbus/
│   ├── WaterbusApplication.java
│   ├── controller/     HomeController + 1 controller / chức năng
│   ├── dto/
│   ├── entity/         12 entity khớp script SQL
│   ├── repository/     12 JpaRepository
│   └── service/        interface  +  impl/ (@Service)
├── src/main/resources/
│   ├── application.properties              (chung, không chứa mật khẩu)
│   ├── application-local.properties.example (mẫu cấu hình riêng mỗi máy)
│   ├── static/css, static/js               (Bootstrap giống project mẫu)
│   └── templates/<trang>/, templates/admin/, templates/fragments/layout.html (chủ: docs/MODULES.md)
├── src/test/               test khởi động app bằng H2
├── AGENTS.md  CLAUDE.md  CONTRIBUTING.md
└── pom.xml  mvnw  mvnw.cmd
```
