package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.entity.Booking;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Dùng chung cho các test BE4 cần tạo đơn.
// createBooking giờ nhận BookingForm và trả BookingDTO (giống controller gọi), nhưng nhiều test cần ENTITY
// để giả lập tình huống (đơn quá hạn, mã KM hết lượt...) -> tạo xong thì lấy lại entity theo mã đơn.
final class BookingTestHelper {

    private BookingTestHelper() {
    }

    // Dựng BookingForm giống form trang checkout gửi lên POST /booking (vé thường, đã tick chính sách)
    static BookingForm form(int tripId, List<Integer> seatIds, int ticketTypeId, int fromStationId, int toStationId,
                            String promoCode, String contactName, String contactPhone, String contactEmail) {
        BookingForm form = new BookingForm();
        form.setTripId(tripId);
        form.setSeatIds(seatIds);
        form.setTicketTypeId(ticketTypeId);
        form.setFromStationId(fromStationId);
        form.setToStationId(toStationId);
        form.setPromoCode(promoCode);
        form.setContactName(contactName);
        form.setContactPhone(contactPhone);
        form.setContactEmail(contactEmail);
        form.setAcceptPolicy(true);
        return form;
    }

    // TR2: giá trị cho session "viewableBookingCodes" = giả lập "chính trình duyệt vừa đặt đơn này",
    // để test mở được trang xem đơn / thanh toán của đơn khách vãng lai.
    // Dùng: .sessionAttr(BookingAccessHelper.VIEWABLE_CODES, BookingTestHelper.viewable(mãĐơn))
    static Set<String> viewable(String bookingCode) {
        Set<String> codes = new HashSet<>();
        codes.add(bookingCode);
        return codes;
    }

    // Tạo đơn đúng cách controller gọi (form + accountId), rồi lấy lại entity của đơn đó cho test dùng tiếp.
    // Thứ tự tham số giữ giống bản createBooking 10 tham số cũ để các test cũ chỉ phải đổi tên hàm.
    static Booking createBooking(BookingService bookingService, int tripId, List<Integer> seatIds, int ticketTypeId,
                                 int fromStationId, int toStationId, String promoCode, Integer accountId,
                                 String contactName, String contactPhone, String contactEmail) {
        BookingForm form = form(tripId, seatIds, ticketTypeId, fromStationId, toStationId,
                promoCode, contactName, contactPhone, contactEmail);
        BookingDTO created = bookingService.createBooking(form, accountId);
        return bookingService.getBookingByCode(created.getBookingCode());
    }
}
