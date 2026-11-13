# Thay đổi database

Không sửa trực tiếp `../WaterbusDB_SQLServer.sql` (file chung, sửa song song sẽ conflict).

Cần thêm/sửa bảng, cột, dữ liệu mẫu:
1. Tạo file mới trong thư mục này: `YYYY-MM-DD_<module>_<mo-ta>.sql`
   VD: `2026-10-21_be3_them-cot-avatar.sql`
2. Viết câu lệnh chạy lại được nhiều lần, VD:
   ```sql
   USE WaterbusDB;
   IF COL_LENGTH('account', 'avatar_url') IS NULL
       ALTER TABLE account ADD avatar_url VARCHAR(255) NULL;
   ```
3. Sửa Entity tương ứng trong cùng PR.
4. Ghi trong mô tả PR: "Cần chạy database/changes/<ten-file>.sql".

Leader định kỳ gộp các file ở đây vào script chính rồi xoá chúng.

## KH3: khởi tạo mới và nâng cấp DB cũ

Script chính trong nhánh KH3 đã chứa schema và seed cuối cùng của các file 22/10, 08/11 và 12/11.
Khởi tạo mới chỉ chạy `../WaterbusDB_SQLServer.sql`; không chạy lại ba migration lịch sử sau đó.
Các file lịch sử được giữ để nâng cấp DB cũ theo mốc đang có, không phải bước bắt buộc của máy mới.

DB đang sử dụng cần CASH/tên WELCOME10 mới thì chạy `2026-11-13_db_tien-mat-ten-khuyen-mai.sql`;
file này không xoá đơn/vé và chạy lại được. Không dùng script xoá-tạo để nâng cấp DB chứa dữ liệu cần giữ.

**Chưa hoàn tất KH3:** phân công tàu trong lịch mẫu còn chờ Trí (TI7). Giữ PR Draft cho tới khi cập nhật lịch
và truy vấn chồng giờ trong `../checks/kiem-tra-du-lieu-mau.sql` trả danh sách rỗng.
