package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDate;
import java.util.List;

// Module: BE2 - Tàu, Ghế & Chuyến
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface TripService {
    /** Tìm chuyến đi qua bến đi rồi tới bến đến trong ngày (màn hình tìm chuyến). */
    List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date);

    /** HỢP ĐỒNG cho BE4: ghế còn trống của chuyến (không có vé HOLD/VALID/USED). */
    List<Seat> getAvailableSeats(int tripId);

    /** HỢP ĐỒNG cho BE4: kiểm tra lại 1 ghế ngay trước khi tạo vé. */
    boolean isSeatAvailable(int tripId, int seatId);
}
