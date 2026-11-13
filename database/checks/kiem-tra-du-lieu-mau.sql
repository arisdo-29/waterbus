-- Chỉ đọc dữ liệu. Lịch TI7 chỉ đạt khi danh sách chồng giờ dưới đây rỗng.
USE WaterbusDB;
GO
SELECT w.code AS waterbus_code, a.trip_code AS first_trip, b.trip_code AS second_trip,
       a.departure_time, a.arrival_time, b.departure_time AS second_departure
FROM trip a
JOIN trip b ON a.waterbus_id = b.waterbus_id AND a.id < b.id
JOIN waterbus w ON w.id = a.waterbus_id
WHERE a.status <> 'CANCELLED' AND b.status <> 'CANCELLED'
  AND a.departure_time < b.arrival_time AND b.departure_time < a.arrival_time
ORDER BY w.code, a.departure_time;

-- Phút phải bắt đầu từ 0 và tăng nghiêm ngặt theo thứ tự bến.
WITH stops AS (
    SELECT route_id, stop_order, minutes_from_start,
           LAG(minutes_from_start) OVER (PARTITION BY route_id ORDER BY stop_order) AS previous_minutes
    FROM route_station
)
SELECT * FROM stops
WHERE (stop_order = 1 AND minutes_from_start <> 0)
   OR minutes_from_start < 0 OR minutes_from_start <= previous_minutes;

-- Mỗi tàu phải có đủ số ghế theo sức chứa.
SELECT w.code, w.capacity, COUNT(s.id) AS active_seats
FROM waterbus w LEFT JOIN seat s ON s.waterbus_id = w.id AND s.is_active = 1
GROUP BY w.id, w.code, w.capacity
HAVING COUNT(s.id) <> w.capacity;
