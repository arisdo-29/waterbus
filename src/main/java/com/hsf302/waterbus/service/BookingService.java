package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.entity.Booking;
import java.util.List;

// Module: BE4 - Đặt vé & Thanh toán
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface BookingService {
    /** Tạo booking PENDING + vé HOLD, expires_at = now + 10 phút. Gọi TripService/TicketTypeService/PromotionService. */
    Booking createBooking(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId, String promoCode, Integer accountId, String contactName, String contactPhone, String contactEmail);

    /** Tra cứu đơn theo mã. */
    Booking getBookingByCode(String bookingCode);

    /** Lịch sử đặt vé của khách. */
    List<Booking> getBookingsOfAccount(int accountId);

    /** Thông tin đơn theo mã để hiển thị (trang xem vé, thanh toán). Không tìm thấy -> null. */
    BookingDTO getBookingDetail(String bookingCode);

    /** Danh sách vé của 1 đơn, theo thứ tự tạo. */
    List<TicketDTO> getTicketsOfBooking(int bookingId);

    /** Chuyến tàu của 1 đơn (mọi vé trong đơn cùng 1 chuyến). Đơn không có vé -> null. */
    BookingTripDTO getTripOfBooking(int bookingId);
}
