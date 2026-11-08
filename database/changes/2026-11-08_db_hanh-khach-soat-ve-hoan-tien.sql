/* =====================================================================
   THÔNG TIN HÀNH KHÁCH, NHẬT KÝ SOÁT VÉ, HOÀN TIỀN, LÝ DO HUỶ CHUYẾN
   ---------------------------------------------------------------------
   Chạy SAU database/WaterbusDB_SQLServer.sql. Chạy lại nhiều lần được.
   Entity đi kèm: Ticket (passengerName, birthYear), Trip (cancelReason),
   TicketScanLog (mới - BE3 soát vé), Refund (mới - BE4 hoàn tiền).

   1. ticket: passenger_name, birth_year (họ tên / năm sinh từng hành khách).
   2. ticket_scan_log: nhật ký mỗi lần STAFF xem / soát / từ chối vé.
   3. refund: mỗi booking tối đa 1 yêu cầu hoàn tiền (PENDING -> DONE).
   4. trip: cancel_reason (lý do huỷ chuyến).

   LƯU Ý khi chạy lại script chính (nó DROP rồi tạo lại các bảng):
   2 bảng mới có FK tới trip/account/booking, nên phải xoá chúng trước:
       DROP TABLE IF EXISTS refund;
       DROP TABLE IF EXISTS ticket_scan_log;
   rồi mới chạy script chính, sau đó chạy lại file này.
   ===================================================================== */
USE WaterbusDB;
GO

/* ---------- 1. ticket: thông tin hành khách ---------- */
IF COL_LENGTH('ticket', 'passenger_name') IS NULL
    ALTER TABLE ticket ADD passenger_name NVARCHAR(100) NULL;
GO
IF COL_LENGTH('ticket', 'birth_year') IS NULL
    ALTER TABLE ticket ADD birth_year INT NULL;
GO
IF OBJECT_ID('CK_ticket_birth_year', 'C') IS NULL
    ALTER TABLE ticket ADD CONSTRAINT CK_ticket_birth_year
        CHECK (birth_year IS NULL OR birth_year >= 1900);
GO

/* ---------- 2. ticket_scan_log: nhật ký soát vé ---------- */
IF OBJECT_ID('ticket_scan_log', 'U') IS NULL
BEGIN
    CREATE TABLE ticket_scan_log (
        id            INT IDENTITY(1,1) NOT NULL,
        ticket_code   VARCHAR(30)       NULL,          -- mã nhập/quét được, có thể sai nên không FK
        booking_code  VARCHAR(20)       NULL,
        trip_id       INT               NULL,
        staff_id      INT               NOT NULL,      -- account STAFF/ADMIN thao tác
        action        VARCHAR(20)       NOT NULL,
        result        VARCHAR(10)       NOT NULL,
        source        VARCHAR(20)       NOT NULL,
        message       NVARCHAR(255)     NULL,
        created_at    DATETIME2(0)      NOT NULL CONSTRAINT DF_scan_created DEFAULT SYSDATETIME(),
        CONSTRAINT PK_ticket_scan_log PRIMARY KEY (id),
        CONSTRAINT FK_scan_trip  FOREIGN KEY (trip_id)  REFERENCES trip(id),
        CONSTRAINT FK_scan_staff FOREIGN KEY (staff_id) REFERENCES account(id),
        CONSTRAINT CK_scan_action CHECK (action IN ('VIEW','CHECKIN','REJECT')),
        CONSTRAINT CK_scan_result CHECK (result IN ('SUCCESS','FAILED')),
        CONSTRAINT CK_scan_source CHECK (source IN ('MANUAL','QR','MANIFEST','OVERRIDE'))
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_scan_staff_created' AND object_id = OBJECT_ID('ticket_scan_log'))
    CREATE INDEX IX_scan_staff_created ON ticket_scan_log(staff_id, created_at);
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_scan_trip' AND object_id = OBJECT_ID('ticket_scan_log'))
    CREATE INDEX IX_scan_trip ON ticket_scan_log(trip_id);
GO

/* ---------- 3. refund: hoàn tiền ---------- */
IF OBJECT_ID('refund', 'U') IS NULL
BEGIN
    CREATE TABLE refund (
        id               INT IDENTITY(1,1) NOT NULL,
        booking_id       INT               NOT NULL,
        amount           DECIMAL(12,0)     NOT NULL,
        status           VARCHAR(10)       NOT NULL CONSTRAINT DF_refund_status DEFAULT 'PENDING',
        reason           NVARCHAR(255)     NOT NULL,
        transaction_ref  VARCHAR(100)      NULL,
        note             NVARCHAR(255)     NULL,
        processed_by     INT               NULL,          -- account ADMIN xử lý
        refunded_at      DATETIME2(0)      NULL,
        created_at       DATETIME2(0)      NOT NULL CONSTRAINT DF_refund_created DEFAULT SYSDATETIME(),
        CONSTRAINT PK_refund PRIMARY KEY (id),
        CONSTRAINT UQ_refund_booking UNIQUE (booking_id),               -- 1 booking - 0..1 refund
        CONSTRAINT FK_refund_booking   FOREIGN KEY (booking_id)   REFERENCES booking(id),
        CONSTRAINT FK_refund_processor FOREIGN KEY (processed_by) REFERENCES account(id),
        CONSTRAINT CK_refund_amount CHECK (amount >= 0),
        CONSTRAINT CK_refund_status CHECK (status IN ('PENDING','DONE'))
    );
END
GO

/* ---------- 4. trip: lý do huỷ chuyến ---------- */
IF COL_LENGTH('trip', 'cancel_reason') IS NULL
    ALTER TABLE trip ADD cancel_reason NVARCHAR(255) NULL;
GO
