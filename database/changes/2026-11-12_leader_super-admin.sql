-- Leader: dữ liệu demo còn thiếu. Chạy lại nhiều lần được (đã có thì bỏ qua).
-- 1) Tài khoản super admin id = 0 (AccountService.SUPER_ADMIN_ID): không bị đổi quyền/khoá/reset mật khẩu.
-- 2) Mã khuyến mãi FREE100: giảm 100% -> đơn 0 đ được PAID ngay, không chuyển sang VNPay.
USE WaterbusDB;
GO

IF NOT EXISTS (SELECT 1 FROM account WHERE id = 0)
BEGIN
    -- Cột id là IDENTITY nên phải bật IDENTITY_INSERT mới ghi được id = 0
    SET IDENTITY_INSERT account ON;
    INSERT INTO account (id, email, password_hash, full_name, phone, role) VALUES
     (0, 'superadmin@waterbus.vn', '$2a$10$ZovUZgJ.m6ejc9p2fYelQueDB3a/rEhDydGii95HwtIAbtKQVXR6u',
      N'Super Admin', '0900000000', 'ADMIN');   -- status, created_at lấy mặc định của bảng
    SET IDENTITY_INSERT account OFF;
END
GO

IF NOT EXISTS (SELECT 1 FROM promotion WHERE code = 'FREE100')
BEGIN
    -- PERCENT 100 hợp lệ với CK_promo_value (<= 100); max_discount NULL = không có trần
    INSERT INTO promotion (code, name, discount_type, discount_value, max_discount, min_order_amount,
                           start_at, end_at, usage_limit) VALUES
     ('FREE100', N'Miễn phí 100% (demo)', 'PERCENT', 100, NULL, 0,
         CAST(SYSDATETIME() AS DATE), DATEADD(MONTH, 1, CAST(SYSDATETIME() AS DATE)), 5);
END
GO
