/* =====================================================================
   CẬP NHẬT DỮ LIỆU BẾN - TUYẾN - LỊCH CHẠY THEO SAIGON WATERBUS THỰC TẾ
   ---------------------------------------------------------------------
   Chạy SAU database/WaterbusDB_SQLServer.sql. Chạy lại nhiều lần được.
   Không đổi schema, không cần sửa Entity.

   Nguồn (tra cứu 22/10/2026):
   - saigonwaterbus.com/dat-ve-truc-tuyen: 8 bến đặt vé được, vé 1 chiều,
     không hoàn/hủy, miễn phí cho người >= 70 tuổi, trẻ < 1 tuổi,
     người khuyết tật, thương binh.
   - saigonwaterbus.com/water-bus: tuyến số 1 dài khoảng 11 km, 15.000đ/lượt.
   - Trang chủ saigonwaterbus.com: lịch chạy áp dụng từ 01/04/2026,
     chuyến "S" chỉ chạy thứ 7, chủ nhật, ngày lễ.
   - Bảng giờ chi tiết (chép lại lịch 01/04/2026): mia.vn, 28/04/2026.
   - Phường mới sau sáp nhập 01/07/2025: Wikipedia "Saigon Waterbus",
     thuvienphapluat.vn (12 phường mới của TP Thủ Đức cũ).

   Thay đổi so với script chính:
   1. Bến: 5 -> 8 bến, đúng thứ tự trên sông (thêm Thủ Thiêm, Ba Son,
      Thảo Điền), địa chỉ theo phường mới. id 1..8 = thứ tự bến, vì trang
      chủ sắp bến theo id. Vé cũ và route_station được đổi id theo.
   2. Số phút giữa các bến lấy theo bảng giờ thật (toàn tuyến 60 phút).
      Lưu ý: trang đặt vé ghi số phút ước tính khác (5/10/15/20/32/42/52).
   3. Tuyến: R1/R1-R = Bạch Đằng <-> Linh Đông (8 bến);
      R2/R2-R đổi thành Bạch Đằng <-> Thảo Điền (chuyến ngắn chạy cả ngày,
      thay cho Bạch Đằng - Bình An cũ); thêm R3/R3-R Bạch Đằng <-> Thủ Thiêm.
   4. Chuyến 7 ngày tới theo giờ thật; chuyến S chỉ sinh vào thứ 7, CN.
      Chuyến đã có vé được giữ nguyên (chỉ tính lại giờ đến).
   Không đổi: loại vé (15.000đ, các đối tượng miễn phí đã đúng), tàu 66 ghế.
   ===================================================================== */
USE WaterbusDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRAN;

/* ---------- 1. BẾN ---------- */
DECLARE @station TABLE (
    slot    INT           PRIMARY KEY,   -- id mong muốn = thứ tự trên tuyến
    code    VARCHAR(20)   NOT NULL UNIQUE,
    name    NVARCHAR(100) NOT NULL,
    address NVARCHAR(255) NOT NULL
);
INSERT INTO @station (slot, code, name, address) VALUES
 (1, 'BD',  N'Bạch Đằng',       N'Bến Bạch Đằng, 10B Tôn Đức Thắng, phường Sài Gòn'),
 (2, 'TT',  N'Thủ Thiêm',       N'Bến tàu Thủ Thiêm, khu đô thị Thủ Thiêm, phường An Khánh'),
 (3, 'BS',  N'Ba Son',          N'Bến tàu Ba Son, cạnh ga metro Ba Son, phường Sài Gòn'),
 (4, 'BA',  N'Bình An',         N'Đường số 21, phường An Khánh'),
 (5, 'TDN', N'Thảo Điền',       N'Bến tàu Thảo Điền, phường An Khánh'),
 (6, 'TD',  N'Thanh Đa',        N'Bán đảo Thanh Đa, phường Bình Quới'),
 (7, 'HBC', N'Hiệp Bình Chánh', N'Gần Giga Mall, phường Hiệp Bình'),
 (8, 'LD',  N'Linh Đông',       N'Bến Linh Đông, phường Thủ Đức');

-- Không tự xử lý được nếu DB đã có bến khác (tạo từ trang admin) chiếm id 1..8
IF EXISTS (SELECT 1 FROM station s
           WHERE s.id BETWEEN 1 AND 8
             AND s.code NOT IN (SELECT code FROM @station)
             AND s.code NOT LIKE 'TMP[_]%')
   OR EXISTS (SELECT 1 FROM station s JOIN @station t ON t.code = s.code WHERE s.id > 8)
    THROW 50001, N'Bảng station có bến lạ ở id 1..8 (hoặc bến chuẩn ở id > 8). Hãy chạy lại database/WaterbusDB_SQLServer.sql rồi chạy file này.', 1;

-- Tạo sẵn các id còn trống (Thủ Thiêm, Ba Son, Thảo Điền) với mã tạm
SET IDENTITY_INSERT station ON;
INSERT INTO station (id, code, name, address)
SELECT t.slot, CONCAT('TMP_', t.slot), t.name, t.address
FROM @station t
WHERE NOT EXISTS (SELECT 1 FROM station s WHERE s.id = t.slot);
SET IDENTITY_INSERT station OFF;

-- id cũ -> id mới của các bến đã có (VD: Bình An 2 -> 4)
DECLARE @map TABLE (old_id INT PRIMARY KEY, new_id INT NOT NULL);
INSERT INTO @map (old_id, new_id)
SELECT s.id, t.slot FROM station s JOIN @station t ON t.code = s.code WHERE s.id <> t.slot;

-- Đổi id bến trong vé đã đặt
UPDATE tk
SET from_station_id = COALESCE(mf.new_id, tk.from_station_id),
    to_station_id   = COALESCE(mt.new_id, tk.to_station_id)
FROM ticket tk
LEFT JOIN @map mf ON mf.old_id = tk.from_station_id
LEFT JOIN @map mt ON mt.old_id = tk.to_station_id
WHERE mf.old_id IS NOT NULL OR mt.old_id IS NOT NULL;

-- Bỏ thứ tự bến của các tuyến sẽ nạp lại; tuyến khác (tạo từ admin) thì đổi id theo
DELETE rs FROM route_station rs
JOIN route r ON r.id = rs.route_id
WHERE r.code IN ('R1', 'R1-R', 'R2', 'R2-R', 'R3', 'R3-R');

UPDATE rs SET station_id = m.new_id
FROM route_station rs JOIN @map m ON m.old_id = rs.station_id;

-- Ghi nội dung bến theo đúng id (đổi mã tạm trước để không trùng UQ_station_code)
UPDATE station SET code = CONCAT('TMP_', id) WHERE id BETWEEN 1 AND 8;
UPDATE s SET code = t.code, name = t.name, address = t.address, is_active = 1
FROM station s JOIN @station t ON t.slot = s.id;

/* ---------- 2. TUYẾN ---------- */
MERGE route AS r
USING (VALUES
    ('R1',   N'Bạch Đằng - Linh Đông', N'Tuyến số 1, chiều đi: 8 bến, khoảng 11 km, khoảng 60 phút'),
    ('R1-R', N'Linh Đông - Bạch Đằng', N'Tuyến số 1, chiều về: 8 bến, khoảng 11 km, khoảng 60 phút'),
    ('R2',   N'Bạch Đằng - Thảo Điền', N'Chuyến ngắn chạy cả ngày tới 23h, chiều đi (5 bến, khoảng 27 phút)'),
    ('R2-R', N'Thảo Điền - Bạch Đằng', N'Chuyến ngắn chạy cả ngày tới 23h, chiều về (5 bến, khoảng 27 phút)'),
    ('R3',   N'Bạch Đằng - Thủ Thiêm', N'Chuyến đưa đón qua sông cuối ngày, chiều đi (khoảng 5 phút)'),
    ('R3-R', N'Thủ Thiêm - Bạch Đằng', N'Chuyến đưa đón qua sông đầu ngày, chiều về (khoảng 5 phút)')
) AS v(code, name, description)
ON r.code = v.code
WHEN MATCHED THEN UPDATE SET name = v.name, description = v.description, is_active = 1
WHEN NOT MATCHED THEN INSERT (code, name, description) VALUES (v.code, v.name, v.description);

/* ---------- 3. THỨ TỰ BẾN + SỐ PHÚT (theo bảng giờ 01/04/2026) ---------- */
INSERT INTO route_station (route_id, station_id, stop_order, minutes_from_start)
SELECT r.id, s.id, v.stop_order, v.minutes
FROM (VALUES
    ('R1','BD',1,0),   ('R1','TT',2,5),   ('R1','BS',3,10),  ('R1','BA',4,22),
    ('R1','TDN',5,27), ('R1','TD',6,40),  ('R1','HBC',7,50), ('R1','LD',8,60),
    ('R1-R','LD',1,0),  ('R1-R','HBC',2,10), ('R1-R','TD',3,20), ('R1-R','TDN',4,33),
    ('R1-R','BA',5,38), ('R1-R','BS',6,50),  ('R1-R','TT',7,55), ('R1-R','BD',8,60),
    ('R2','BD',1,0),   ('R2','TT',2,5),   ('R2','BS',3,10),  ('R2','BA',4,22),  ('R2','TDN',5,27),
    ('R2-R','TDN',1,0),('R2-R','BA',2,5), ('R2-R','BS',3,17),('R2-R','TT',4,22),('R2-R','BD',5,27),
    ('R3','BD',1,0),   ('R3','TT',2,5),
    ('R3-R','TT',1,0), ('R3-R','BD',2,5)
) AS v(route_code, station_code, stop_order, minutes)
JOIN route   r ON r.code = v.route_code
JOIN station s ON s.code = v.station_code;

/* ---------- 4. CHUYẾN: 7 ngày tính từ hôm nay ---------- */
-- Giữ chuyến đã có vé, chỉ tính lại giờ đến theo số phút mới
UPDATE t
SET arrival_time = DATEADD(MINUTE,
        (SELECT MAX(rs.minutes_from_start) FROM route_station rs WHERE rs.route_id = t.route_id),
        t.departure_time)
FROM trip t JOIN route r ON r.id = t.route_id
WHERE r.code IN ('R1', 'R1-R', 'R2', 'R2-R', 'R3', 'R3-R')
  AND EXISTS (SELECT 1 FROM ticket tk WHERE tk.trip_id = t.id);

-- Xóa chuyến chưa có vé của các tuyến này rồi sinh lại
DELETE t FROM trip t JOIN route r ON r.id = t.route_id
WHERE r.code IN ('R1', 'R1-R', 'R2', 'R2-R', 'R3', 'R3-R')
  AND NOT EXISTS (SELECT 1 FROM ticket tk WHERE tk.trip_id = t.id);

-- weekend_only = 1: chuyến "S", chỉ chạy thứ 7, chủ nhật (và ngày lễ - không xét ở đây)
-- Mã D/S ghi trong cột cuối để đối chiếu với lịch thật, không lưu vào DB.
;WITH days AS (
    SELECT 0 AS d UNION ALL SELECT d + 1 FROM days WHERE d < 6
),
sched AS (
    SELECT * FROM (VALUES
        -- Tuyến số 1, chiều đi (Bạch Đằng -> Linh Đông)
        ('R1',  'WB01', '08:30', 0, 'D1'),  ('R1',  'WB02', '09:30', 1, 'S1'),
        ('R1',  'WB01', '11:00', 0, 'D7'),  ('R1',  'WB01', '14:30', 0, 'D11'),
        ('R1',  'WB01', '17:10', 0, 'D19'),
        -- Tuyến số 1, chiều về (Linh Đông -> Bạch Đằng)
        ('R1-R','WB01', '09:45', 0, 'D4'),  ('R1-R','WB02', '11:00', 1, 'S2'),
        ('R1-R','WB01', '13:15', 0, 'D8'),  ('R1-R','WB01', '16:00', 0, 'D14'),
        ('R1-R','WB01', '18:25', 0, 'D20'),
        -- Chuyến ngắn Bạch Đằng -> Thảo Điền
        ('R2',  'WB02', '09:00', 0, 'D3'),  ('R2',  'WB02', '10:30', 0, 'D5'),
        ('R2',  'WB02', '13:00', 1, 'S3'),  ('R2',  'WB02', '13:30', 0, 'D9'),
        ('R2',  'WB02', '15:00', 1, 'S5'),  ('R2',  'WB02', '15:30', 0, 'D13'),
        ('R2',  'WB02', '16:30', 0, 'D15'), ('R2',  'WB02', '17:00', 0, 'D17'),
        ('R2',  'WB02', '18:10', 0, 'D21'), ('R2',  'WB02', '18:45', 0, 'D23'),
        ('R2',  'WB02', '19:30', 0, 'D25'), ('R2',  'WB02', '19:45', 1, 'S7'),
        ('R2',  'WB02', '20:15', 0, 'D27'), ('R2',  'WB02', '21:15', 0, 'D29'),
        ('R2',  'WB02', '21:45', 0, 'D31'), ('R2',  'WB02', '22:00', 1, 'S9'),
        ('R2',  'WB02', '22:45', 0, 'D33'),
        -- Chuyến ngắn Thảo Điền -> Bạch Đằng
        ('R2-R','WB02', '09:40', 0, 'D2'),  ('R2-R','WB02', '11:15', 0, 'D6'),
        ('R2-R','WB02', '14:00', 1, 'S4'),  ('R2-R','WB02', '14:30', 0, 'D10'),
        ('R2-R','WB02', '15:45', 1, 'S6'),  ('R2-R','WB02', '16:15', 0, 'D12'),
        ('R2-R','WB02', '17:15', 0, 'D16'), ('R2-R','WB02', '18:00', 0, 'D18'),
        ('R2-R','WB02', '18:50', 0, 'D22'), ('R2-R','WB02', '19:30', 0, 'D24'),
        ('R2-R','WB02', '20:30', 0, 'D26'), ('R2-R','WB02', '20:45', 1, 'S8'),
        ('R2-R','WB02', '21:00', 0, 'D28'), ('R2-R','WB02', '22:00', 0, 'D30'),
        ('R2-R','WB02', '22:30', 0, 'D32'), ('R2-R','WB02', '22:45', 1, 'S10'),
        -- Đưa đón Bạch Đằng <-> Thủ Thiêm (lịch thật không đặt mã chuyến)
        ('R3',  'WB01', '23:10', 0, '-'),   ('R3',  'WB01', '23:20', 0, '-'),
        ('R3-R','WB01', '08:15', 0, '-'),   ('R3-R','WB01', '08:40', 0, '-')
    ) AS s(route_code, waterbus_code, dep, weekend_only, real_code)
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
    -- 1900-01-01 là thứ 2 => % 7 = 5 là thứ 7, 6 là chủ nhật (không phụ thuộc SET DATEFIRST)
    WHERE s.weekend_only = 0
       OR DATEDIFF(DAY, '19000101', DATEADD(DAY, d.d, CAST(SYSDATETIME() AS DATE))) % 7 IN (5, 6)
)
INSERT INTO trip (trip_code, route_id, waterbus_id, departure_time, arrival_time, status)
SELECT CONCAT(CONVERT(CHAR(8), x.departure_time, 112), '-', x.route_code, '-', REPLACE(x.dep, ':', '')),
       x.route_id, x.waterbus_id, x.departure_time, DATEADD(MINUTE, x.duration, x.departure_time), 'SCHEDULED'
FROM x
WHERE NOT EXISTS (SELECT 1 FROM trip t
                  WHERE t.trip_code = CONCAT(CONVERT(CHAR(8), x.departure_time, 112), '-', x.route_code, '-', REPLACE(x.dep, ':', '')));

COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
GO

/* ---------- KIỂM TRA NHANH ---------- */
SELECT id, code, name, address FROM station ORDER BY id;          -- 8 bến, id 1..8 đúng thứ tự
SELECT r.code, s.name, rs.stop_order, rs.minutes_from_start
FROM route_station rs JOIN route r ON r.id = rs.route_id JOIN station s ON s.id = rs.station_id
ORDER BY r.id, rs.stop_order;
SELECT r.code, COUNT(*) AS so_chuyen_7_ngay
FROM trip t JOIN route r ON r.id = t.route_id GROUP BY r.id, r.code ORDER BY r.id;
GO
