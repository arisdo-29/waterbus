USE WaterbusDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
SET NOCOUNT ON;
GO

/*
  Dữ liệu mẫu cho trang doanh thu: 30 ngày gần nhất (không gồm hôm nay).
  - Thêm 8 khách hàng, 2 khuyến mãi đã hết hạn, các chuyến đã chạy (COMPLETED).
  - Mỗi ngày có số đơn khác nhau: cuối tuần đông hơn, có vài ngày vắng và vài ngày không có đơn.
  - Đơn có đủ trạng thái PAID / CANCELLED / EXPIRED; thanh toán VNPAY và CASH;
    vé thường, vé miễn phí (SENIOR_70, PRIORITY); có nhật ký soát vé.
  - Hoàn tiền CHỈ phát sinh khi hủy chuyến: 3 chuyến bị hủy, vé trả tiền của chuyến đó được hoàn
    (2 khoản đã hoàn, 1 khoản đang chờ xác nhận). Khách tự mua rồi không được hoàn.
  Chạy lại nhiều lần được: đã có đơn BK90000001 thì bỏ qua. Không xoá dữ liệu có sẵn.
*/

IF EXISTS (SELECT 1 FROM booking WHERE booking_code = 'BK90000001')
BEGIN
    PRINT N'Dữ liệu mẫu doanh thu đã có, bỏ qua.';
    RETURN;
END

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @today DATE = CAST(SYSDATETIME() AS DATE);
    DECLARE @days INT = 30;
    DECLARE @std DECIMAL(12,0) = ISNULL((SELECT price FROM ticket_type WHERE code = 'STANDARD'), 15000);
    DECLARE @staff INT = ISNULL((SELECT id FROM account WHERE email = 'staff@waterbus.vn'),
                                (SELECT TOP 1 id FROM account WHERE role IN ('STAFF', 'ADMIN') ORDER BY id));

    DROP TABLE IF EXISTS #slot;
    DROP TABLE IF EXISTS #day;
    DROP TABLE IF EXISTS #num;
    DROP TABLE IF EXISTS #cust;
    DROP TABLE IF EXISTS #guest;
    DROP TABLE IF EXISTS #plan;
    DROP TABLE IF EXISTS #bk;
    DROP TABLE IF EXISTS #cx;

    -- 1. Khách hàng
    INSERT INTO account (email, password_hash, full_name, phone, role, created_at)
    SELECT v.email, '$2a$10$7K3UB/0aoVCKOX29nBbndO3nQmwCwumb11EHBibUaRAJ7pMVLG5dK', v.full_name, v.phone,
           'CUSTOMER', DATEADD(DAY, -(@days + 10), CAST(SYSDATETIME() AS DATETIME2(0)))
    FROM (VALUES
        ('khachhang01@gmail.com', N'Trần Minh Khoa',   '0901000001'),
        ('khachhang02@gmail.com', N'Lê Thị Thu Hà',    '0901000002'),
        ('khachhang03@gmail.com', N'Phạm Quốc Bảo',    '0901000003'),
        ('khachhang04@gmail.com', N'Võ Ngọc Anh',      '0901000004'),
        ('khachhang05@gmail.com', N'Đặng Hoàng Long',  '0901000005'),
        ('khachhang06@gmail.com', N'Bùi Thanh Tâm',    '0901000006'),
        ('khachhang07@gmail.com', N'Huỳnh Gia Hân',    '0901000007'),
        ('khachhang08@gmail.com', N'Nguyễn Đức Thịnh', '0901000008')
    ) AS v(email, full_name, phone)
    WHERE NOT EXISTS (SELECT 1 FROM account a WHERE a.email = v.email);

    SELECT ROW_NUMBER() OVER (ORDER BY a.id) AS n, a.id, a.full_name, a.phone, a.email
    INTO #cust
    FROM account a
    WHERE a.email = 'customer@gmail.com' OR a.email LIKE 'khachhang0_@gmail.com';
    DECLARE @nc INT = (SELECT COUNT(*) FROM #cust);

    SELECT * INTO #guest FROM (VALUES
        (1, N'Nguyễn Thị Lan'), (2, N'Trần Văn Hùng'), (3, N'Lê Hoàng Yến'), (4, N'Phạm Minh Tuấn'),
        (5, N'Hoàng Thu Trang'), (6, N'Đỗ Quang Vinh'), (7, N'Vũ Khánh Linh'), (8, N'Ngô Bảo Châu')
    ) AS g(n, name);

    -- 2. Khuyến mãi đã hết hạn (dùng cho các đơn quá khứ)
    INSERT INTO promotion (code, name, discount_type, discount_value, max_discount, min_order_amount,
                           start_at, end_at, usage_limit, is_active)
    SELECT v.code, v.name, v.discount_type, v.discount_value, v.max_discount, v.min_order_amount,
           DATEADD(DAY, -(@days + 5), @today), DATEADD(DAY, -1, @today), NULL, 1
    FROM (VALUES
        ('KHAITRUONG10', N'Khai trương giảm 10%, tối đa 10.000 đ', 'PERCENT', 10, 10000, 0),
        ('GIAM3K',       N'Giảm 3.000 đ cho đơn từ 30.000 đ',      'FIXED',    3000, NULL, 30000)
    ) AS v(code, name, discount_type, discount_value, max_discount, min_order_amount)
    WHERE NOT EXISTS (SELECT 1 FROM promotion p WHERE p.code = v.code);

    -- 3. Lịch chạy mẫu (8 chuyến/ngày) và các ngày cần tạo
    SELECT * INTO #slot FROM (VALUES
        (1, 'R1',   'WB01', '08:30'), (2, 'R1-R', 'WB01', '09:45'),
        (3, 'R1',   'WB01', '14:30'), (4, 'R1-R', 'WB01', '16:00'),
        (5, 'R2',   'WB02', '09:00'), (6, 'R2-R', 'WB02', '11:15'),
        (7, 'R2',   'WB02', '17:00'), (8, 'R2-R', 'WB02', '18:00')
    ) AS s(idx, route_code, waterbus_code, dep);

    ;WITH n AS (SELECT 1 AS d UNION ALL SELECT d + 1 FROM n WHERE d < 30)
    SELECT d, DATEADD(DAY, -d, @today) AS dt,
           DATEDIFF(DAY, '19000101', DATEADD(DAY, -d, @today)) % 7 AS dow
    INTO #day
    FROM n;

    SELECT * INTO #num FROM (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12)) AS v(n);

    INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status)
    SELECT x.trip_code, r.id, w.id, x.dep_time, DATEADD(MINUTE, m.duration, x.dep_time), 'COMPLETED'
    FROM (
        SELECT s.route_code, s.waterbus_code,
               CONCAT(CONVERT(CHAR(8), dy.dt, 112), '-', s.route_code, '-', REPLACE(s.dep, ':', '')) AS trip_code,
               DATEADD(MINUTE, DATEPART(HOUR, CAST(s.dep AS TIME)) * 60 + DATEPART(MINUTE, CAST(s.dep AS TIME)),
                       CAST(dy.dt AS DATETIME2(0))) AS dep_time
        FROM #slot s CROSS JOIN #day dy
    ) x
    JOIN route r    ON r.code = x.route_code
    JOIN waterbus w ON w.code = x.waterbus_code
    CROSS APPLY (SELECT MAX(rs.minutes_from_start) AS duration FROM route_station rs WHERE rs.route_id = r.id) m
    WHERE NOT EXISTS (SELECT 1 FROM trip t WHERE t.trip_code = x.trip_code);

    -- 4. Kế hoạch đơn theo ngày: cuối tuần đông, có ngày vắng, có ngày trống
    CREATE TABLE #plan (
        seq INT IDENTITY(1,1) PRIMARY KEY,
        d INT, dt DATE, slot_idx INT,
        kind_roll INT, qty_roll INT, promo_roll INT, method_roll INT, acct_roll INT,
        route_roll INT, stop_roll INT, minute_roll INT, use_roll INT, refund_roll INT
    );

    INSERT INTO #plan (d, dt, slot_idx, kind_roll, qty_roll, promo_roll, method_roll, acct_roll,
                       route_roll, stop_roll, minute_roll, use_roll, refund_roll)
    SELECT dy.d, dy.dt, 1 + h.a % 8, h.b % 100, h.c % 100, h.e % 100, h.f % 100, h.g % 100,
           h.i, h.j, h.k, h.l % 100, h.m
    FROM #day dy
    CROSS APPLY (SELECT cnt = CASE
                     WHEN dy.d % 11 = 6 THEN 0
                     WHEN dy.d % 9 = 4 THEN 1
                     ELSE (CASE WHEN dy.dow IN (5, 6) THEN 6 WHEN dy.dow = 4 THEN 4 ELSE 2 END)
                          + (CHECKSUM(dy.d, 5) & 2147483647) % 3 + (@days - dy.d) / 10
                 END) c
    JOIN #num nm ON nm.n <= c.cnt
    CROSS APPLY (SELECT a = CHECKSUM(NEWID()) & 2147483647,
                        b = CHECKSUM(NEWID()) & 2147483647,
                        c = CHECKSUM(NEWID()) & 2147483647,
                        e = CHECKSUM(NEWID()) & 2147483647,
                        f = CHECKSUM(NEWID()) & 2147483647,
                        g = CHECKSUM(NEWID()) & 2147483647,
                        i = CHECKSUM(NEWID()) & 2147483647,
                        j = CHECKSUM(NEWID()) & 2147483647,
                        k = CHECKSUM(NEWID()) & 2147483647,
                        l = CHECKSUM(NEWID()) & 2147483647,
                        m = CHECKSUM(NEWID()) & 2147483647) h
    ORDER BY dy.d DESC, nm.n;

    SELECT p.seq, p.d, p.acct_roll, p.method_roll, p.minute_roll, p.use_roll, p.refund_roll,
           CONCAT('BK9', RIGHT(CONCAT('0000000', p.seq), 7)) AS booking_code,
           t.id AS trip_id, t.route_id, t.waterbus_id, t.departure_time,
           k.kind, q.qty, f.free_qty, st.subtotal, pr.promo, di.discount,
           fo.from_ord, fo.from_ord + 1 + p.stop_roll % (rc.c - fo.from_ord) AS to_ord
    INTO #bk
    FROM #plan p
    JOIN #slot s ON s.idx = p.slot_idx
    JOIN trip t ON t.trip_code = CONCAT(CONVERT(CHAR(8), p.dt, 112), '-', s.route_code, '-', REPLACE(s.dep, ':', ''))
    CROSS APPLY (SELECT kind = CASE WHEN p.kind_roll < 70 THEN 'PAID'
                                    WHEN p.kind_roll < 78 THEN 'MIX'
                                    WHEN p.kind_roll < 82 THEN 'FREE'
                                    WHEN p.kind_roll < 89 THEN 'CANCELLED'
                                    WHEN p.kind_roll < 94 THEN 'EXPIRED'
                                    ELSE 'PAID' END) k
    CROSS APPLY (SELECT qty = CASE WHEN k.kind = 'MIX'  THEN 2 + p.qty_roll % 3
                                   WHEN k.kind = 'FREE' THEN 1 + p.qty_roll % 2
                                   WHEN p.qty_roll < 45 THEN 1
                                   WHEN p.qty_roll < 75 THEN 2
                                   WHEN p.qty_roll < 92 THEN 3
                                   ELSE 4 END) q
    CROSS APPLY (SELECT free_qty = CASE WHEN k.kind = 'MIX' THEN 1 WHEN k.kind = 'FREE' THEN q.qty ELSE 0 END) f
    CROSS APPLY (SELECT subtotal = (q.qty - f.free_qty) * @std) st
    CROSS APPLY (SELECT promo = CASE WHEN k.kind = 'PAID' AND p.promo_roll < 30
                                     THEN CASE WHEN q.qty >= 2 THEN 'GIAM3K' ELSE 'KHAITRUONG10' END END) pr
    CROSS APPLY (SELECT discount = CASE pr.promo
                     WHEN 'GIAM3K' THEN 3000
                     WHEN 'KHAITRUONG10' THEN CASE WHEN ROUND(st.subtotal * 0.1, 0) > 10000 THEN 10000
                                                   ELSE CAST(ROUND(st.subtotal * 0.1, 0) AS INT) END
                     ELSE 0 END) di
    CROSS APPLY (SELECT c = (SELECT COUNT(*) FROM route_station rs WHERE rs.route_id = t.route_id)) rc
    CROSS APPLY (SELECT from_ord = 1 + p.route_roll % (rc.c - 1)) fo;

    -- 5. Đơn đặt vé
    INSERT INTO booking (booking_code, account_id, promotion_id, contact_name, contact_phone, contact_email,
                         subtotal, discount_amount, total_amount, status, created_at, expires_at, paid_at)
    SELECT b.booking_code, c.id, pm.id,
           COALESCE(c.full_name, g.name),
           COALESCE(c.phone, CONCAT('09', RIGHT(CONCAT('00000000', CAST(b.seq * 7919 AS VARCHAR(12))), 8))),
           COALESCE(c.email, CONCAT('khach', b.seq, '@gmail.com')),
           b.subtotal, b.discount, b.subtotal - b.discount,
           CASE b.kind WHEN 'CANCELLED' THEN 'CANCELLED' WHEN 'EXPIRED' THEN 'EXPIRED'
                       ELSE 'PAID' END,
           x.created_at, DATEADD(MINUTE, 10, x.created_at),
           CASE WHEN b.kind IN ('CANCELLED', 'EXPIRED') THEN NULL
                WHEN b.kind = 'FREE' THEN x.created_at
                ELSE DATEADD(MINUTE, 2 + b.method_roll % 6, x.created_at) END
    FROM #bk b
    CROSS APPLY (SELECT created_at = DATEADD(MINUTE, -(45 + b.minute_roll % 180), b.departure_time)) x
    LEFT JOIN #cust c ON c.n = 1 + b.acct_roll % @nc AND b.acct_roll < 75
    LEFT JOIN #guest g ON g.n = 1 + b.seq % 8
    LEFT JOIN promotion pm ON pm.code = b.promo
    ORDER BY b.seq;

    -- 6. Vé
    ;WITH s AS (
        SELECT b.*, SUM(b.qty) OVER (PARTITION BY b.trip_id ORDER BY b.seq ROWS UNBOUNDED PRECEDING) - b.qty AS seat_offset
        FROM #bk b
    ),
    seat_rank AS (
        SELECT id, waterbus_id, ROW_NUMBER() OVER (PARTITION BY waterbus_id ORDER BY seat_row, seat_col) AS rn
        FROM seat WHERE is_active = 1
    )
    INSERT INTO ticket (ticket_code, booking_id, trip_id, seat_id, ticket_type_id, from_station_id, to_station_id,
                        price, status, checked_in_at, passenger_name, birth_year)
    SELECT CONCAT('TK9', RIGHT(CONCAT('0000000', s.seq), 7), '-', nm.n),
           bk.id, s.trip_id, sr.id, tt.id, fs.station_id, ts.station_id,
           CASE WHEN fr.is_free = 1 THEN 0 ELSE @std END,
           CASE WHEN s.kind IN ('CANCELLED', 'EXPIRED') THEN 'CANCELLED'
                WHEN s.use_roll < 92 THEN 'USED' ELSE 'VALID' END,
           CASE WHEN s.kind IN ('CANCELLED', 'EXPIRED') OR s.use_roll >= 92 THEN NULL
                ELSE DATEADD(MINUTE, -(3 + s.minute_roll % 20), s.departure_time) END,
           CASE WHEN fr.is_free = 1 THEN g.name END,
           CASE WHEN fr.is_free = 1 THEN YEAR(@today) - CASE tt.code WHEN 'SENIOR_70' THEN 71 + s.minute_roll % 10
                                                                    ELSE 40 + s.minute_roll % 20 END END
    FROM s
    JOIN booking bk ON bk.booking_code = s.booking_code
    JOIN #num nm ON nm.n <= s.qty
    CROSS APPLY (SELECT is_free = CASE WHEN nm.n <= s.free_qty THEN 1 ELSE 0 END) fr
    JOIN seat_rank sr ON sr.waterbus_id = s.waterbus_id AND sr.rn = s.seat_offset + nm.n
    JOIN ticket_type tt ON tt.code = CASE WHEN fr.is_free = 0 THEN 'STANDARD'
                                          WHEN s.kind = 'MIX' THEN 'SENIOR_70'
                                          WHEN (s.seq + nm.n) % 2 = 0 THEN 'SENIOR_70'
                                          ELSE 'PRIORITY' END
    JOIN route_station rf ON rf.route_id = s.route_id AND rf.stop_order = s.from_ord
    JOIN route_station rt ON rt.route_id = s.route_id AND rt.stop_order = s.to_ord
    CROSS APPLY (SELECT station_id = rf.station_id) fs
    CROSS APPLY (SELECT station_id = rt.station_id) ts
    LEFT JOIN #guest g ON g.n = 1 + (s.seq + nm.n) % 8;

    -- 7. Thanh toán (đơn có tiền: VNPAY hoặc tiền mặt)
    INSERT INTO payment (booking_id, method, amount, status, transaction_ref, created_at, paid_at)
    SELECT bk.id, m.method, bk.total_amount, 'SUCCESS',
           CASE WHEN m.method = 'VNPAY' THEN CONCAT('14', RIGHT(CONCAT('000000', b.seq), 6), '0') END,
           bk.created_at, bk.paid_at
    FROM #bk b
    JOIN booking bk ON bk.booking_code = b.booking_code
    CROSS APPLY (SELECT method = CASE WHEN b.method_roll < 62 THEN 'VNPAY' ELSE 'CASH' END) m
    WHERE b.kind IN ('PAID', 'MIX');

    -- 8. Hủy chuyến: 3 chuyến bị hủy, vé trả tiền được hoàn (đúng luồng createForCancelledTrip)
    SELECT r.idx, r.trip_id, r.d, r.departure_time
    INTO #cx
    FROM (
        SELECT NTILE(3) OVER (ORDER BY q.d) AS idx, q.trip_id, q.d, q.departure_time,
               ROW_NUMBER() OVER (PARTITION BY q.d ORDER BY q.paying DESC, q.trip_id) AS rn_day
        FROM (
            SELECT b.trip_id, b.d, b.departure_time, COUNT(*) AS paying
            FROM #bk b
            WHERE b.kind IN ('PAID', 'MIX') AND b.d >= 3
            GROUP BY b.trip_id, b.d, b.departure_time
            HAVING COUNT(*) >= 2
        ) q
    ) r
    WHERE r.rn_day = 1;

    -- mỗi nhóm chỉ giữ 1 chuyến: idx 1 = gần nhất (hoàn tiền chờ xác nhận), idx 2, 3 = đã hoàn
    DELETE c FROM #cx c
    WHERE c.d <> (SELECT MIN(c2.d) FROM #cx c2 WHERE c2.idx = c.idx);

    UPDATE t SET status = 'CANCELLED',
                 cancel_reason = CASE x.idx WHEN 1 THEN N'Thời tiết xấu, sóng lớn trên sông'
                                            WHEN 2 THEN N'Tàu gặp sự cố kỹ thuật'
                                            ELSE N'Mực nước thấp, tạm dừng khai thác' END
    FROM trip t JOIN #cx x ON x.trip_id = t.id;

    UPDATE tk SET status = 'CANCELLED', checked_in_at = NULL
    FROM ticket tk
    JOIN #cx x ON x.trip_id = tk.trip_id
    WHERE tk.ticket_code LIKE 'TK9000%';

    -- vé 0 đồng: huỷ đơn, không hoàn tiền
    UPDATE bk SET status = 'CANCELLED'
    FROM booking bk
    JOIN #bk b ON b.booking_code = bk.booking_code
    JOIN #cx x ON x.trip_id = b.trip_id
    WHERE b.kind = 'FREE';

    -- vé có tiền: tạo khoản hoàn
    INSERT INTO refund (booking_id, amount, status, reason, transaction_ref, note, processed_by, refunded_at, created_at)
    SELECT bk.id, bk.total_amount,
           CASE WHEN x.idx = 1 THEN 'PENDING' ELSE 'DONE' END,
           CONCAT(N'Hủy chuyến: ', t.cancel_reason),
           CASE WHEN x.idx > 1 AND p.method = 'VNPAY' THEN CONCAT('RF', RIGHT(CONCAT('000000', b.seq), 6)) END,
           NULL,
           CASE WHEN x.idx > 1 THEN @staff END,
           CASE WHEN x.idx > 1 THEN DATEADD(MINUTE, 600 + b.refund_roll % 300, x.departure_time) END,
           x.departure_time
    FROM #bk b
    JOIN #cx x ON x.trip_id = b.trip_id
    JOIN trip t ON t.id = x.trip_id
    JOIN booking bk ON bk.booking_code = b.booking_code
    JOIN payment p ON p.booking_id = bk.id
    WHERE b.kind IN ('PAID', 'MIX');

    UPDATE bk SET status = 'REFUNDED'
    FROM booking bk
    JOIN refund r ON r.booking_id = bk.id AND r.status = 'DONE'
    WHERE bk.booking_code LIKE 'BK9000%';

    -- 9. Nhật ký soát vé cho vé đã dùng
    INSERT INTO ticket_scan_log (ticket_code, booking_code, trip_id, staff_id, action, result, source, message, created_at)
    SELECT t.ticket_code, bk.booking_code, t.trip_id, @staff, 'CHECKIN', 'SUCCESS',
           CASE t.id % 3 WHEN 0 THEN 'QR' WHEN 1 THEN 'MANIFEST' ELSE 'MANUAL' END,
           NULL, t.checked_in_at
    FROM ticket t
    JOIN booking bk ON bk.id = t.booking_id
    WHERE t.ticket_code LIKE 'TK9000%' AND t.status = 'USED' AND t.checked_in_at IS NOT NULL;

    -- 10. Đồng bộ số lượt dùng khuyến mãi
    UPDATE p SET used_count = (SELECT COUNT(*) FROM booking b
                               WHERE b.promotion_id = p.id AND b.status = 'PAID'
                                 AND NOT EXISTS (SELECT 1 FROM refund r WHERE r.booking_id = b.id))
    FROM promotion p
    WHERE p.code IN ('KHAITRUONG10', 'GIAM3K');

    COMMIT;

    SELECT CONVERT(CHAR(10), CAST(p.paid_at AS DATE), 120) AS ngay, COUNT(*) AS so_don, SUM(p.amount) AS doanh_thu
    FROM payment p
    WHERE p.status = 'SUCCESS' AND p.paid_at >= DATEADD(DAY, -@days, @today) AND p.paid_at < @today
    GROUP BY CAST(p.paid_at AS DATE)
    ORDER BY ngay;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO
