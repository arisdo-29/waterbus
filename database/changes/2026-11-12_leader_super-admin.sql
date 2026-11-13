USE WaterbusDB;
GO

IF NOT EXISTS (SELECT 1 FROM account WHERE id = 0)
BEGIN
    SET IDENTITY_INSERT account ON;
    INSERT INTO account (id, email, password_hash, full_name, phone, role) VALUES
     (0, 'superadmin@waterbus.vn', '$2a$10$ZovUZgJ.m6ejc9p2fYelQueDB3a/rEhDydGii95HwtIAbtKQVXR6u',
      N'Super Admin', '0900000000', 'ADMIN');
    SET IDENTITY_INSERT account OFF;
END
GO

IF NOT EXISTS (SELECT 1 FROM promotion WHERE code = 'FREE100')
BEGIN
    INSERT INTO promotion (code, name, discount_type, discount_value, max_discount, min_order_amount,
                           start_at, end_at, usage_limit) VALUES
     ('FREE100', N'Miễn phí 100% (demo)', 'PERCENT', 100, NULL, 0,
         CAST(SYSDATETIME() AS DATE), DATEADD(MONTH, 1, CAST(SYSDATETIME() AS DATE)), 5);
END
GO
