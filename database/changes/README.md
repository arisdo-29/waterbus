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
