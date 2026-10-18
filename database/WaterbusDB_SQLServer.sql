/* =====================================================================
   SAIGON WATERBUS - Database script cho SQL Server (2016 trở lên)
   Dự án HSF302 - Spring Boot 4.1.1 + Spring Data JPA
   ---------------------------------------------------------------------
   Cách chạy: mở file trong SSMS (hoặc Azure Data Studio), bấm Execute.
   Script chạy lại được nhiều lần: nó xóa bảng cũ rồi tạo lại.
   Dữ liệu mẫu (giờ chạy, số phút giữa các bến) chỉ để test, nhóm tự chỉnh.
   FILE CHUNG - thay đổi schema: tạo file mới trong database/changes/
   ===================================================================== */

IF DB_ID(N'WaterbusDB') IS NULL
    CREATE DATABASE WaterbusDB;
GO
USE WaterbusDB;
GO

/* ---------- Xóa bảng cũ (theo thứ tự con -> cha) ---------- */
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS promotion;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS ticket_type;
DROP TABLE IF EXISTS trip;
DROP TABLE IF EXISTS seat;
DROP TABLE IF EXISTS waterbus;
DROP TABLE IF EXISTS route_station;
DROP TABLE IF EXISTS route;
DROP TABLE IF EXISTS station;
GO

/* =====================================================================
   1. NHÓM TUYẾN - BẾN
   ===================================================================== */
CREATE TABLE station (
    id          INT IDENTITY(1,1) NOT NULL,
    code        VARCHAR(20)       NOT NULL,
    name        NVARCHAR(100)     NOT NULL,
    address     NVARCHAR(255)     NULL,
    is_active   BIT               NOT NULL CONSTRAINT DF_station_active DEFAULT 1,
    CONSTRAINT PK_station PRIMARY KEY (id),
    CONSTRAINT UQ_station_code UNIQUE (code)
);

-- Mỗi chiều đi là 1 route riêng (VD: Bạch Đằng -> Linh Đông và Linh Đông -> Bạch Đằng)
CREATE TABLE route (
    id          INT IDENTITY(1,1) NOT NULL,
    code        VARCHAR(20)       NOT NULL,
    name        NVARCHAR(100)     NOT NULL,
    description NVARCHAR(255)     NULL,
    is_active   BIT               NOT NULL CONSTRAINT DF_route_active DEFAULT 1,
    CONSTRAINT PK_route PRIMARY KEY (id),
    CONSTRAINT UQ_route_code UNIQUE (code)
);

-- Bảng trung gian route <-> station, có thứ tự bến
CREATE TABLE route_station (
    id                  INT IDENTITY(1,1) NOT NULL,
    route_id            INT NOT NULL,
    station_id          INT NOT NULL,
    stop_order          INT NOT NULL,          -- 1, 2, 3 ...
    minutes_from_start  INT NOT NULL CONSTRAINT DF_rs_minutes DEFAULT 0,
    CONSTRAINT PK_route_station PRIMARY KEY (id),
    CONSTRAINT FK_rs_route   FOREIGN KEY (route_id)   REFERENCES route(id),
    CONSTRAINT FK_rs_station FOREIGN KEY (station_id) REFERENCES station(id),
    CONSTRAINT UQ_rs_route_order   UNIQUE (route_id, stop_order),
    CONSTRAINT UQ_rs_route_station UNIQUE (route_id, station_id),
    CONSTRAINT CK_rs_order   CHECK (stop_order >= 1),
    CONSTRAINT CK_rs_minutes CHECK (minutes_from_start >= 0)
);

/* =====================================================================
   2. NHÓM TÀU - GHẾ - CHUYẾN
   ===================================================================== */
CREATE TABLE waterbus (
    id          INT IDENTITY(1,1) NOT NULL,
    code        VARCHAR(20)       NOT NULL,
    name        NVARCHAR(100)     NOT NULL,
    capacity    INT               NOT NULL,
    status      VARCHAR(20)       NOT NULL CONSTRAINT DF_waterbus_status DEFAULT 'ACTIVE',
    CONSTRAINT PK_waterbus PRIMARY KEY (id),
    CONSTRAINT UQ_waterbus_code UNIQUE (code),
    CONSTRAINT CK_waterbus_capacity CHECK (capacity > 0),
    CONSTRAINT CK_waterbus_status CHECK (status IN ('ACTIVE','MAINTENANCE','INACTIVE'))
);

CREATE TABLE seat (
    id           INT IDENTITY(1,1) NOT NULL,
    waterbus_id  INT              NOT NULL,
    seat_code    VARCHAR(5)       NOT NULL,      -- VD: A1, A2 ... K6
    seat_row     INT              NOT NULL,      -- dùng để vẽ sơ đồ ghế
    seat_col     INT              NOT NULL,
    is_active    BIT              NOT NULL CONSTRAINT DF_seat_active DEFAULT 1,
    CONSTRAINT PK_seat PRIMARY KEY (id),
    CONSTRAINT FK_seat_waterbus FOREIGN KEY (waterbus_id) REFERENCES waterbus(id),
    CONSTRAINT UQ_seat_code UNIQUE (waterbus_id, seat_code)
);

CREATE TABLE trip (
    id              INT IDENTITY(1,1) NOT NULL,
    trip_code       VARCHAR(20)       NOT NULL,
    route_id        INT               NOT NULL,
    waterbus_id     INT               NOT NULL,
    departure_time  DATETIME2(0)      NOT NULL,
    arrival_time    DATETIME2(0)      NOT NULL,
    status          VARCHAR(20)       NOT NULL CONSTRAINT DF_trip_status DEFAULT 'SCHEDULED',
    CONSTRAINT PK_trip PRIMARY KEY (id),
    CONSTRAINT UQ_trip_code UNIQUE (trip_code),
    CONSTRAINT FK_trip_route    FOREIGN KEY (route_id)    REFERENCES route(id),
    CONSTRAINT FK_trip_waterbus FOREIGN KEY (waterbus_id) REFERENCES waterbus(id),
    CONSTRAINT CK_trip_time   CHECK (arrival_time > departure_time),
    CONSTRAINT CK_trip_status CHECK (status IN ('SCHEDULED','DEPARTED','COMPLETED','CANCELLED'))
);
CREATE INDEX IX_trip_route_departure ON trip(route_id, departure_time);

/* =====================================================================
   3. NHÓM VÉ - ĐẶT VÉ - THANH TOÁN - KHUYẾN MÃI
   ===================================================================== */
CREATE TABLE ticket_type (
    id                     INT IDENTITY(1,1) NOT NULL,
    code                   VARCHAR(30)       NOT NULL,
    name                   NVARCHAR(100)     NOT NULL,
    price                  DECIMAL(12,0)     NOT NULL,
    requires_verification  BIT               NOT NULL CONSTRAINT DF_tt_verify DEFAULT 0,
    description            NVARCHAR(255)     NULL,
    is_active              BIT               NOT NULL CONSTRAINT DF_tt_active DEFAULT 1,
    CONSTRAINT PK_ticket_type PRIMARY KEY (id),
    CONSTRAINT UQ_ticket_type_code UNIQUE (code),
    CONSTRAINT CK_ticket_type_price CHECK (price >= 0)
);

CREATE TABLE account (
    id             INT IDENTITY(1,1) NOT NULL,
    email          VARCHAR(100)      NOT NULL,
    password_hash  VARCHAR(100)      NOT NULL,   -- BCrypt, KHÔNG lưu mật khẩu thô
    full_name      NVARCHAR(100)     NOT NULL,
    phone          VARCHAR(15)       NULL,
    role           VARCHAR(20)       NOT NULL CONSTRAINT DF_account_role DEFAULT 'CUSTOMER',
    status         VARCHAR(20)       NOT NULL CONSTRAINT DF_account_status DEFAULT 'ACTIVE',
    created_at     DATETIME2(0)      NOT NULL CONSTRAINT DF_account_created DEFAULT SYSDATETIME(),
    CONSTRAINT PK_account PRIMARY KEY (id),
    CONSTRAINT UQ_account_email UNIQUE (email),
    CONSTRAINT CK_account_role   CHECK (role IN ('CUSTOMER','STAFF','ADMIN')),
    CONSTRAINT CK_account_status CHECK (status IN ('ACTIVE','LOCKED'))
);

CREATE TABLE promotion (
    id                INT IDENTITY(1,1) NOT NULL,
    code              VARCHAR(30)       NOT NULL,
    name              NVARCHAR(100)     NOT NULL,
    discount_type     VARCHAR(10)       NOT NULL,     -- PERCENT | FIXED
    discount_value    DECIMAL(12,2)     NOT NULL,
    max_discount      DECIMAL(12,0)     NULL,         -- trần giảm khi PERCENT
    min_order_amount  DECIMAL(12,0)     NOT NULL CONSTRAINT DF_promo_min DEFAULT 0,
    start_at          DATETIME2(0)      NOT NULL,
    end_at            DATETIME2(0)      NOT NULL,
    usage_limit       INT               NULL,         -- NULL = không giới hạn
    used_count        INT               NOT NULL CONSTRAINT DF_promo_used DEFAULT 0,
    is_active         BIT               NOT NULL CONSTRAINT DF_promo_active DEFAULT 1,
    CONSTRAINT PK_promotion PRIMARY KEY (id),
    CONSTRAINT UQ_promotion_code UNIQUE (code),
    CONSTRAINT CK_promo_type   CHECK (discount_type IN ('PERCENT','FIXED')),
    CONSTRAINT CK_promo_value  CHECK (discount_value > 0 AND (discount_type = 'FIXED' OR discount_value <= 100)),
    CONSTRAINT CK_promo_period CHECK (end_at > start_at),
    CONSTRAINT CK_promo_usage  CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

CREATE TABLE booking (
    id               INT IDENTITY(1,1) NOT NULL,
    booking_code     VARCHAR(20)       NOT NULL,
    account_id       INT               NULL,          -- NULL = khách vãng lai
    promotion_id     INT               NULL,
    contact_name     NVARCHAR(100)     NOT NULL,
    contact_phone    VARCHAR(15)       NOT NULL,
    contact_email    VARCHAR(100)      NOT NULL,
    subtotal         DECIMAL(12,0)     NOT NULL,
    discount_amount  DECIMAL(12,0)     NOT NULL CONSTRAINT DF_booking_discount DEFAULT 0,
    total_amount     DECIMAL(12,0)     NOT NULL,
    status           VARCHAR(20)       NOT NULL CONSTRAINT DF_booking_status DEFAULT 'PENDING',
    created_at       DATETIME2(0)      NOT NULL CONSTRAINT DF_booking_created DEFAULT SYSDATETIME(),
    expires_at       DATETIME2(0)      NOT NULL,      -- hạn giữ ghế (VD: created_at + 10 phút)
    paid_at          DATETIME2(0)      NULL,
    CONSTRAINT PK_booking PRIMARY KEY (id),
    CONSTRAINT UQ_booking_code UNIQUE (booking_code),
    CONSTRAINT FK_booking_account   FOREIGN KEY (account_id)   REFERENCES account(id),
    CONSTRAINT FK_booking_promotion FOREIGN KEY (promotion_id) REFERENCES promotion(id),
    CONSTRAINT CK_booking_amount CHECK (subtotal >= 0 AND discount_amount >= 0
                                        AND total_amount = subtotal - discount_amount),
    CONSTRAINT CK_booking_status CHECK (status IN ('PENDING','PAID','EXPIRED','CANCELLED','REFUNDED'))
);
CREATE INDEX IX_booking_status_expires ON booking(status, expires_at);   -- job hủy đơn quá hạn
CREATE INDEX IX_booking_account ON booking(account_id);

CREATE TABLE payment (
    id               INT IDENTITY(1,1) NOT NULL,
    booking_id       INT               NOT NULL,
    method           VARCHAR(20)       NOT NULL,      -- MOCK | VNPAY | MOMO
    amount           DECIMAL(12,0)     NOT NULL,
    status           VARCHAR(20)       NOT NULL CONSTRAINT DF_payment_status DEFAULT 'PENDING',
    transaction_ref  VARCHAR(100)      NULL,          -- mã giao dịch cổng thanh toán trả về
    created_at       DATETIME2(0)      NOT NULL CONSTRAINT DF_payment_created DEFAULT SYSDATETIME(),
    paid_at          DATETIME2(0)      NULL,
    CONSTRAINT PK_payment PRIMARY KEY (id),
    CONSTRAINT UQ_payment_booking UNIQUE (booking_id),               -- 1 booking - 0..1 payment
    CONSTRAINT FK_payment_booking FOREIGN KEY (booking_id) REFERENCES booking(id),
    CONSTRAINT CK_payment_method CHECK (method IN ('MOCK','VNPAY','MOMO')),
    CONSTRAINT CK_payment_status CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    CONSTRAINT CK_payment_amount CHECK (amount >= 0)
);

CREATE TABLE ticket (
    id               INT IDENTITY(1,1) NOT NULL,
    ticket_code      VARCHAR(20)       NOT NULL,      -- in lên QR để soát vé
    booking_id       INT               NOT NULL,
    trip_id          INT               NOT NULL,
    seat_id          INT               NOT NULL,
    ticket_type_id   INT               NOT NULL,
    from_station_id  INT               NOT NULL,
    to_station_id    INT               NOT NULL,
    price            DECIMAL(12,0)     NOT NULL,      -- chụp lại giá lúc mua
    status           VARCHAR(20)       NOT NULL CONSTRAINT DF_ticket_status DEFAULT 'HOLD',
    checked_in_at    DATETIME2(0)      NULL,
    CONSTRAINT PK_ticket PRIMARY KEY (id),
    CONSTRAINT UQ_ticket_code UNIQUE (ticket_code),
    CONSTRAINT FK_ticket_booking      FOREIGN KEY (booking_id)      REFERENCES booking(id),
    CONSTRAINT FK_ticket_trip         FOREIGN KEY (trip_id)         REFERENCES trip(id),
    CONSTRAINT FK_ticket_seat         FOREIGN KEY (seat_id)         REFERENCES seat(id),
    CONSTRAINT FK_ticket_type         FOREIGN KEY (ticket_type_id)  REFERENCES ticket_type(id),
    CONSTRAINT FK_ticket_from_station FOREIGN KEY (from_station_id) REFERENCES station(id),
    CONSTRAINT FK_ticket_to_station   FOREIGN KEY (to_station_id)   REFERENCES station(id),
    CONSTRAINT CK_ticket_stations CHECK (from_station_id <> to_station_id),
    CONSTRAINT CK_ticket_price    CHECK (price >= 0),
    CONSTRAINT CK_ticket_status   CHECK (status IN ('HOLD','VALID','USED','CANCELLED'))
);
-- CHỐNG ĐẶT TRÙNG GHẾ: 1 ghế trên 1 chuyến chỉ có 1 vé đang hiệu lực.
-- Vé CANCELLED không bị tính nên ghế được nhả ra cho người khác.
CREATE UNIQUE INDEX UX_ticket_trip_seat_active
    ON ticket(trip_id, seat_id)
    WHERE status IN ('HOLD','VALID','USED');
CREATE INDEX IX_ticket_booking ON ticket(booking_id);
GO

/* =====================================================================
   4. DỮ LIỆU MẪU
   ===================================================================== */

-- Bến (theo tuyến Bạch Đằng - Linh Đông)
INSERT INTO station (code, name, address) VALUES
 ('BD',  N'Bạch Đằng',        N'Tôn Đức Thắng, Bến Nghé, Quận 1'),
 ('BA',  N'Bình An',          N'Đường số 21, Bình An, TP. Thủ Đức'),
 ('TD',  N'Thanh Đa',         N'Bình Quới, Bình Thạnh'),
 ('HBC', N'Hiệp Bình Chánh',  N'Hiệp Bình Chánh, TP. Thủ Đức'),
 ('LD',  N'Linh Đông',        N'Linh Đông, TP. Thủ Đức');

-- Tuyến: mỗi chiều 1 route
INSERT INTO route (code, name, description) VALUES
 ('R1',   N'Bạch Đằng - Linh Đông', N'Tuyến số 1, chiều đi'),
 ('R1-R', N'Linh Đông - Bạch Đằng', N'Tuyến số 1, chiều về'),
 ('R2',   N'Bạch Đằng - Bình An',   N'Chuyến ngắn buổi tối, chiều đi'),
 ('R2-R', N'Bình An - Bạch Đằng',   N'Chuyến ngắn buổi tối, chiều về');

-- Thứ tự bến (số phút là số liệu giả định để test)
INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start)
SELECT r.id, s.id, v.stop_order, v.minutes
FROM (VALUES
    ('R1','BD',1,0),  ('R1','BA',2,10),  ('R1','TD',3,25),  ('R1','HBC',4,40),  ('R1','LD',5,52),
    ('R1-R','LD',1,0),('R1-R','HBC',2,12),('R1-R','TD',3,27),('R1-R','BA',4,42),('R1-R','BD',5,52),
    ('R2','BD',1,0),  ('R2','BA',2,10),
    ('R2-R','BA',1,0),('R2-R','BD',2,10)
) AS v(route_code, station_code, stop_order, minutes)
JOIN route   r ON r.code = v.route_code
JOIN station s ON s.code = v.station_code;

-- Tàu
INSERT INTO waterbus (code, name, capacity, status) VALUES
 ('WB01', N'Waterbus 01', 66, 'ACTIVE'),
 ('WB02', N'Waterbus 02', 66, 'ACTIVE'),
 ('WB03', N'Waterbus 03', 66, 'MAINTENANCE');

-- Ghế: mỗi tàu 66 ghế = 11 hàng (A..K) x 6 cột
INSERT INTO seat (waterbus_id, seat_code, seat_row, seat_col)
SELECT w.id, CONCAT(CHAR(64 + r.n), c.n), r.n, c.n
FROM waterbus w
CROSS JOIN (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11)) AS r(n)
CROSS JOIN (VALUES (1),(2),(3),(4),(5),(6)) AS c(n);

-- Loại vé (giá đồng giá 15.000đ/lượt, một số đối tượng được miễn phí)
INSERT INTO ticket_type (code, name, price, requires_verification, description) VALUES
 ('STANDARD',  N'Vé thường',               15000, 0, N'Áp dụng cho mọi chặng'),
 ('SENIOR_70', N'Người từ 70 tuổi trở lên', 0,     1, N'Xuất trình CCCD khi lên tàu'),
 ('PRIORITY',  N'Người khuyết tật, thương binh', 0, 1, N'Xuất trình giấy tờ khi lên tàu');

-- Chuyến: 7 ngày tính từ hôm nay (giờ chạy là số liệu giả định)
;WITH days AS (
    SELECT 0 AS d UNION ALL SELECT d + 1 FROM days WHERE d < 6
),
sched AS (
    SELECT * FROM (VALUES
        ('R1',  'WB01', '07:00'), ('R1',  'WB01', '10:30'), ('R1',  'WB01', '14:00'), ('R1',  'WB01', '17:30'),
        ('R1-R','WB01', '08:15'), ('R1-R','WB01', '11:45'), ('R1-R','WB01', '15:15'), ('R1-R','WB01', '18:45'),
        ('R2',  'WB02', '17:00'), ('R2',  'WB02', '18:00'), ('R2',  'WB02', '19:00'), ('R2',  'WB02', '20:00'),
        ('R2-R','WB02', '17:30'), ('R2-R','WB02', '18:30'), ('R2-R','WB02', '19:30'), ('R2-R','WB02', '20:30')
    ) AS s(route_code, waterbus_code, dep)
),
x AS (
    SELECT r.id AS route_id, w.id AS waterbus_id, r.code AS route_code, s.dep,
           DATEADD(MINUTE, DATEPART(HOUR, CAST(s.dep AS TIME)) * 60 + DATEPART(MINUTE, CAST(s.dep AS TIME)),
                   CAST(DATEADD(DAY, d.d, CAST(SYSDATETIME() AS DATE)) AS DATETIME2(0))) AS departure_time,
           (SELECT MAX(rs.minutes_from_start) FROM route_station rs WHERE rs.route_id = r.id) AS duration
    FROM days d
    CROSS JOIN sched s
    JOIN route r    ON r.code = s.route_code
    JOIN waterbus w ON w.code = s.waterbus_code
)
INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status)
SELECT CONCAT(CONVERT(CHAR(8), departure_time, 112), '-', route_code, '-', REPLACE(dep, ':', '')),
       route_id, waterbus_id, departure_time, DATEADD(MINUTE, duration, departure_time), 'SCHEDULED'
FROM x;

-- Tài khoản mẫu (mật khẩu đã mã hóa BCrypt)
--   admin@waterbus.vn    / Admin@123
--   staff@waterbus.vn    / Staff@123
--   customer@gmail.com   / Customer@123
INSERT INTO account (email, password_hash, full_name, phone, role) VALUES
 ('admin@waterbus.vn',  '$2a$10$fmRV08iLfd4U7d3KQI4V9.ucz1nf61tCzlyPMeaqkdOKaoYFQkftG', N'Quản trị viên', '0900000001', 'ADMIN'),
 ('staff@waterbus.vn',  '$2a$10$HNNXWkyVXfQ/B9k8XNi9Hut9AN5HIGZS0VlejEeJF52NNAtojv.7a', N'Nhân viên bến', '0900000002', 'STAFF'),
 ('customer@gmail.com', '$2a$10$7K3UB/0aoVCKOX29nBbndO3nQmwCwumb11EHBibUaRAJ7pMVLG5dK', N'Nguyễn Văn A',  '0900000003', 'CUSTOMER');

-- Khuyến mãi mẫu
INSERT INTO promotion (code, name, discount_type, discount_value, max_discount, min_order_amount,
                       start_at, end_at, usage_limit) VALUES
 ('WELCOME10', N'Giảm 10% cho đơn đầu tiên', 'PERCENT', 10, 10000, 0,
     CAST(SYSDATETIME() AS DATE), DATEADD(MONTH, 3, CAST(SYSDATETIME() AS DATE)), 100),
 ('GIAM5K',    N'Giảm 5.000đ cho đơn từ 30.000đ', 'FIXED', 5000, NULL, 30000,
     CAST(SYSDATETIME() AS DATE), DATEADD(MONTH, 1, CAST(SYSDATETIME() AS DATE)), NULL);

-- 1 đơn mẫu ĐÃ THANH TOÁN: 2 vé thường, chuyến R1 gần nhất, ghế A1, A2, chặng Bạch Đằng -> Thanh Đa
DECLARE @trip INT = (SELECT TOP 1 t.id FROM trip t JOIN route r ON r.id = t.route_id
                     WHERE r.code = 'R1' AND t.departure_time > SYSDATETIME() ORDER BY t.departure_time);
DECLARE @bus INT  = (SELECT waterbus_id FROM trip WHERE id = @trip);
DECLARE @acc INT  = (SELECT id FROM account WHERE email = 'customer@gmail.com');

INSERT INTO booking (booking_code, account_id, promotion_id, contact_name, contact_phone, contact_email,
                     subtotal, discount_amount, total_amount, status, created_at, expires_at, paid_at)
VALUES ('BK0000001', @acc, NULL, N'Nguyễn Văn A', '0900000003', 'customer@gmail.com',
        30000, 0, 30000, 'PAID', SYSDATETIME(), DATEADD(MINUTE, 10, SYSDATETIME()), SYSDATETIME());

DECLARE @bk INT = SCOPE_IDENTITY();

INSERT INTO payment (booking_id, method, amount, status, transaction_ref, paid_at)
VALUES (@bk, 'MOCK', 30000, 'SUCCESS', 'MOCK-0000001', SYSDATETIME());

INSERT INTO ticket (ticket_code, booking_id, trip_id, seat_id, ticket_type_id,
                    from_station_id, to_station_id, price, status)
SELECT CONCAT('TK000000', ROW_NUMBER() OVER (ORDER BY s.seat_code)), @bk, @trip, s.id,
       (SELECT id FROM ticket_type WHERE code = 'STANDARD'),
       (SELECT id FROM station WHERE code = 'BD'),
       (SELECT id FROM station WHERE code = 'TD'),
       15000, 'VALID'
FROM seat s
WHERE s.waterbus_id = @bus AND s.seat_code IN ('A1', 'A2');
GO

/* =====================================================================
   5. KIỂM TRA NHANH
   ===================================================================== */
SELECT 'station' AS tbl, COUNT(*) AS total FROM station
UNION ALL SELECT 'route', COUNT(*) FROM route
UNION ALL SELECT 'route_station', COUNT(*) FROM route_station
UNION ALL SELECT 'waterbus', COUNT(*) FROM waterbus
UNION ALL SELECT 'seat', COUNT(*) FROM seat
UNION ALL SELECT 'trip', COUNT(*) FROM trip
UNION ALL SELECT 'ticket_type', COUNT(*) FROM ticket_type
UNION ALL SELECT 'account', COUNT(*) FROM account
UNION ALL SELECT 'promotion', COUNT(*) FROM promotion
UNION ALL SELECT 'booking', COUNT(*) FROM booking
UNION ALL SELECT 'ticket', COUNT(*) FROM ticket;

-- Ghế còn trống của 1 chuyến (câu truy vấn mẫu cho màn hình chọn ghế)
-- SELECT s.* FROM seat s
-- JOIN trip t ON t.waterbus_id = s.waterbus_id AND t.id = @tripId
-- WHERE s.is_active = 1
--   AND NOT EXISTS (SELECT 1 FROM ticket tk WHERE tk.trip_id = t.id AND tk.seat_id = s.id
--                   AND tk.status IN ('HOLD','VALID','USED'));
