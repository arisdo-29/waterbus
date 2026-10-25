package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import java.math.BigDecimal;
import java.util.List;

// Module: BE4 - Đặt vé & Thanh toán
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface BookingService {
    /** Tạo booking PENDING + vé HOLD, expires_at = now + 10 phút. Gọi TripService/TicketTypeService/PromotionService. */
    Booking createBooking(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId, String promoCode, Integer accountId, String contactName, String contactPhone, String contactEmail);

    /** Tra cứu đơn theo mã. */
    Booking getBookingByCode(String bookingCode);

    /** Lịch sử đặt vé của khách, đơn mới nhất lên đầu. Chưa có đơn -> danh sách rỗng. */
    List<BookingDTO> getBookingsOfAccount(int accountId);

    /** Thông tin đơn theo mã để hiển thị (trang xem vé, thanh toán). Không tìm thấy -> null. */
    BookingDTO getBookingDetail(String bookingCode);

    /** Danh sách vé của 1 đơn, theo thứ tự tạo. */
    List<TicketDTO> getTicketsOfBooking(int bookingId);

    /** Chuyến tàu của 1 đơn (mọi vé trong đơn cùng 1 chuyến). Đơn không có vé -> null. */
    BookingTripDTO getTripOfBooking(int bookingId);

    /** Trong các ghế khách đã chọn, lấy những ghế vẫn còn trống (trang checkout). */
    List<Seat> getSelectedSeats(int tripId, List<Integer> seatIds);

    /** Tạm tính = giá loại vé x số ghế. */
    BigDecimal calculateSubtotal(int ticketTypeId, int seatCount);

    /** Thông tin chuyến để hiển thị trên trang checkout (qua TripService.getBookableTrip). Không tìm thấy hoặc chuyến không còn SCHEDULED -> null. */
    BookingTripDTO getTripInfo(int tripId);

    /** true nếu TẤT CẢ ghế đã chọn còn trống (kiểm tra từng ghế bằng TripService.isSeatAvailable). */
    boolean areSeatsAvailable(int tripId, List<Integer> seatIds);

    /** Nhả ghế của 1 đơn: các vé HOLD -> CANCELLED. Dùng chung cho job hết hạn và chức năng huỷ đơn. */
    void releaseHoldTickets(int bookingId);

    /** Khách huỷ đơn chưa thanh toán: booking -> CANCELLED và nhả ghế. Không hợp lệ -> IllegalArgumentException. */
    void cancelBooking(String bookingCode, Integer accountId);
}
