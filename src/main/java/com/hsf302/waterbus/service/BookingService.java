package com.hsf302.waterbus.service;

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
}
