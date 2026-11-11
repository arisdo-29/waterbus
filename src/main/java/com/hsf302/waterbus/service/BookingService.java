package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.TicketDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.Errors;

// Module: BE4 - Đặt vé & Thanh toán
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface BookingService {
    // Số ghế tối đa trong 1 đơn. Trang chọn ghế (app.js) cũng chặn 10 ghế, nhưng request có thể bị sửa tay
    // nên server phải tự kiểm tra lại. Dùng chung cho BookingForm (@Size) và createBooking.
    int MAX_SEATS_PER_BOOKING = 10;

    /**
     * Tạo booking PENDING + vé HOLD, expires_at = now + 10 phút. Gọi TripService/TicketTypeService/PromotionService.
     * Nhận nguyên form trang checkout (chuyến, ghế, loại vé, bến, mã KM, thông tin liên hệ) thay cho 10-12 tham số rời.
     * Thêm họ tên + năm sinh từng hành khách (vé ưu đãi; vé thường để null).
     * Phần tử thứ i của 2 danh sách thuộc ghế thứ i trong seatIds.
     * Tổng tiền = 0 đ -> đơn PAID + vé VALID ngay, không tạo payment.
     * accountId: tài khoản đang đăng nhập, null = khách vãng lai. Trả DTO cho controller (không trả entity).
     */
    BookingDTO createBooking(BookingForm form, Integer accountId);

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

    String getTripCancelReason(int bookingId);

    /** Trong các ghế khách đã chọn, lấy những ghế vẫn còn trống (trang checkout). */
    List<Seat> getSelectedSeats(int tripId, List<Integer> seatIds);

    /** Tạm tính = giá loại vé x số ghế. */
    BigDecimal calculateSubtotal(int ticketTypeId, int seatCount);

    /**
     * Thông tin chuyến cho trang checkout, giờ tính theo chặng khách chọn (qua TripService.getBookableTrip).
     * null nếu: không có chuyến, chuyến không còn SCHEDULED, chuyến không đi qua chặng này,
     * hoặc đã ngừng bán online (tàu sắp tới bến đi).
     */
    BookingTripDTO getTripInfo(int tripId, int fromStationId, int toStationId);

    /**
     * Vé ưu đãi (loại vé requiresVerification = true): kiểm tra họ tên + năm sinh của từng ghế.
     * Sai ở đâu thì ghi lỗi vào đúng ô đó trong errors (VD "birthYears[1]"). Vé thường -> không kiểm tra gì.
     */
    void validatePassengers(BookingForm form, Errors errors);

    /** true nếu TẤT CẢ ghế đã chọn còn trống (kiểm tra từng ghế bằng TripService.isSeatAvailable). */
    boolean areSeatsAvailable(int tripId, List<Integer> seatIds);

    /** Nhả ghế của 1 đơn: các vé HOLD -> CANCELLED. Dùng chung cho job hết hạn và chức năng huỷ đơn. */
    void releaseHoldTickets(int bookingId);

    /** Khách huỷ đơn chưa thanh toán: booking -> CANCELLED và nhả ghế. Không hợp lệ -> IllegalArgumentException. */
    void cancelBooking(String bookingCode, Integer accountId);
}
