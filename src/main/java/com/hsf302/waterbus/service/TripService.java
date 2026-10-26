package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.AdminTripDTO;
import com.hsf302.waterbus.dto.AdminTripForm;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDate;
import java.util.List;

// Module: BE2 - Tàu, Ghế & Chuyến
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface TripService {
    List<AdminTripDTO> getTripsForAdmin();

    void createAdminTrip(AdminTripForm form);

    void cancelTrip(int tripId);

    /** Tìm chuyến đi qua bến đi rồi tới bến đến trong ngày (màn hình tìm chuyến). */
    List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date);

    List<TripSearchDTO> searchTripResults(int fromStationId, int toStationId, LocalDate date);

    /** Thông tin chuyến cho trang chọn ghế; null nếu không có chuyến hoặc chuyến không SCHEDULED. */
    BookingTripDTO getBookableTrip(int tripId);

    /** HỢP ĐỒNG cho BE4: lấy chuyến theo id để gắn vào vé; null nếu không có. */
    Trip getTripById(int tripId);

    /** Sơ đồ ghế của chuyến: mọi ghế đang dùng của tàu, available = còn trống. */
    List<SeatDTO> getSeatMap(int tripId);

    /** HỢP ĐỒNG cho BE4: ghế còn trống của chuyến (không có vé HOLD/VALID/USED). */
    List<Seat> getAvailableSeats(int tripId);

    /** HỢP ĐỒNG cho BE4: kiểm tra lại 1 ghế ngay trước khi tạo vé. */
    boolean isSeatAvailable(int tripId, int seatId);
}
