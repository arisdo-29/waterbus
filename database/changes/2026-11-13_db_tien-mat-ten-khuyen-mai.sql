-- Nâng cấp DB đang dùng, không xoá đơn/vé. Chạy lại được.
-- KH3: CASH dùng cho TR6; giữ MOCK/MOMO để đọc dữ liệu cũ.
USE WaterbusDB;
GO
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID('dbo.CK_payment_method', 'C') IS NOT NULL
        ALTER TABLE dbo.payment DROP CONSTRAINT CK_payment_method;
    ALTER TABLE dbo.payment WITH CHECK ADD CONSTRAINT CK_payment_method
        CHECK (method IN ('MOCK','VNPAY','MOMO','CASH'));
    UPDATE dbo.promotion SET name = N'Giảm 10%, tối đa 10.000 đ' WHERE code = 'WELCOME10';
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO
