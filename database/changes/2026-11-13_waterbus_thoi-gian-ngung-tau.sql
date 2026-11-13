USE WaterbusDB;
GO

IF COL_LENGTH('waterbus', 'pending_status') IS NULL
    ALTER TABLE waterbus ADD pending_status VARCHAR(20) NULL;
IF COL_LENGTH('waterbus', 'status_starts_at') IS NULL
    ALTER TABLE waterbus ADD status_starts_at DATETIME2(0) NULL;
IF COL_LENGTH('waterbus', 'status_ends_at') IS NULL
    ALTER TABLE waterbus ADD status_ends_at DATETIME2(0) NULL;
GO

IF OBJECT_ID('CK_waterbus_pending_status', 'C') IS NULL
    ALTER TABLE waterbus ADD CONSTRAINT CK_waterbus_pending_status
        CHECK (pending_status IS NULL OR pending_status IN ('MAINTENANCE', 'INACTIVE'));
GO
