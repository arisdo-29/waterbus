package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.StationService;
import com.hsf302.waterbus.service.TicketTypeService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE4 - Đặt vé & Thanh toán. Mọi URL của controller này bắt đầu bằng /booking
// View đặt trong templates/booking/ (giao diện: Khuyên)
@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    // Module khác (chỉ đọc để hiển thị trang checkout): tên bến, loại vé
    private final StationService stationService;
    private final TicketTypeService ticketTypeService;

    public BookingController(BookingService bookingService, StationService stationService,
                             TicketTypeService ticketTypeService) {
        this.bookingService = bookingService;
        this.stationService = stationService;
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping
    public String index() {
        // TODO BE4: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "booking/index";
    }

    // POST /booking/checkout: form ở trang chọn ghế (trip/seats) gửi lên -> trang xem lại đơn, CHƯA lưu gì vào DB
    @PostMapping("/checkout")
    public String checkout(@RequestParam int tripId,
                           @RequestParam(required = false) List<Integer> seatIds, // nhiều checkbox cùng tên
                           @RequestParam int ticketTypeId,
                           @RequestParam int fromStationId,
                           @RequestParam int toStationId,
                           Model model, RedirectAttributes flash) {
        // Địa chỉ trang chọn ghế để quay lại khi có lỗi (URL theo docs/PAGES.md)
        String seatPage = "redirect:/trips/" + tripId + "/seats?fromStationId=" + fromStationId
                + "&toStationId=" + toStationId;
        if (seatIds == null || seatIds.isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất 1 ghế.");
            return seatPage;
        }
        if (!bookingService.areSeatsAvailable(tripId, seatIds)) {
            flash.addFlashAttribute("errorMessage", "Ghế bạn chọn vừa có người đặt. Vui lòng chọn ghế khác.");
            return seatPage;
        }
        // Ghế đều còn trống -> đưa đủ dữ liệu cho trang checkout (subtotal = giá vé x số ghế)
        fillCheckoutModel(model, tripId, seatIds, ticketTypeId, fromStationId, toStationId);
        return "booking/checkout";
    }

    // POST /booking: form ở trang checkout gửi lên -> tạo đơn (tên field theo docs/PAGES.md)
    @PostMapping
    public String create(@RequestParam int tripId,
                         @RequestParam(required = false) List<Integer> seatIds,
                         @RequestParam int ticketTypeId,
                         @RequestParam int fromStationId,
                         @RequestParam int toStationId,
                         @RequestParam(defaultValue = "") String contactName,
                         @RequestParam(defaultValue = "") String contactPhone,
                         @RequestParam(defaultValue = "") String contactEmail,
                         @RequestParam(defaultValue = "") String promoCode,
                         HttpSession session, Model model) {
        // Lấy tài khoản đang đăng nhập (Tân lưu vào session tên "loginAccount" khi đăng nhập)
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        // Đã đăng nhập -> id tài khoản; chưa đăng nhập -> null (khách vãng lai)
        Integer accountId = (loginAccount == null) ? null : loginAccount.getId();
        try {
            Booking booking = bookingService.createBooking(tripId, seatIds, ticketTypeId, fromStationId,
                    toStationId, promoCode, accountId, contactName, contactPhone, contactEmail);
            // Thành công -> sang trang thanh toán. Dùng redirect để bấm F5 không tạo đơn lần 2
            return "redirect:/payment/" + booking.getBookingCode();
        } catch (IllegalArgumentException ex) {
            // Lỗi -> hiện lại trang checkout, KHÔNG redirect để giữ dữ liệu đã nhập.
            // Họ tên/SĐT/email/mã KM tự giữ vì template đọc ${param...} của chính request này.
            fillCheckoutModel(model, tripId, seatIds, ticketTypeId, fromStationId, toStationId);
            model.addAttribute("errorMessage", ex.getMessage());
            return "booking/checkout";
        }
    }

    // GET /booking/BK0000001 -> trang xem vé của đơn BK0000001
    @GetMapping("/{bookingCode}")
    public String detail(@PathVariable String bookingCode, Model model, RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            // Sai mã đơn -> về trang chủ và báo lỗi (fragment alerts hiển thị errorMessage)
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        // Tên attribute đúng theo docs/PAGES.md: booking, trip, tickets
        model.addAttribute("booking", booking);
        model.addAttribute("trip", bookingService.getTripOfBooking(booking.getId()));
        model.addAttribute("tickets", bookingService.getTicketsOfBooking(booking.getId()));
        return "booking/detail";
    }

    // POST /booking/BK12345678/cancel -> nút "Huỷ đơn" (chỉ hiện khi đơn đang PENDING, xem docs/PAGES.md)
    @PostMapping("/{bookingCode}/cancel")
    public String cancel(@PathVariable String bookingCode, HttpSession session, RedirectAttributes flash) {
        // Lấy tài khoản đang đăng nhập giống create(): chưa đăng nhập -> null
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer accountId = (loginAccount == null) ? null : loginAccount.getId();
        try {
            bookingService.cancelBooking(bookingCode, accountId);
            flash.addFlashAttribute("successMessage", "Đã huỷ đơn.");
        } catch (IllegalArgumentException ex) {
            // Không tìm thấy đơn / đơn đã thanh toán / không phải đơn của mình -> báo lỗi đỏ
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        // Quay về trang xem đơn. Spring tự điền {bookingCode} bằng giá trị trên URL hiện tại
        return "redirect:/booking/{bookingCode}";
    }

    // Đưa đủ dữ liệu cho trang booking/checkout (tên attribute theo docs/PAGES.md). Dùng chung cho checkout() và create().
    private void fillCheckoutModel(Model model, int tripId, List<Integer> seatIds, int ticketTypeId,
                                   int fromStationId, int toStationId) {
        List<Seat> seats = bookingService.getSelectedSeats(tripId, seatIds);
        model.addAttribute("trip", bookingService.getTripInfo(tripId));
        model.addAttribute("seats", seats);
        model.addAttribute("ticketType", ticketTypeService.getTicketTypeById(ticketTypeId));
        model.addAttribute("subtotal", bookingService.calculateSubtotal(ticketTypeId, seats.size()));
        model.addAttribute("fromStation", stationService.getStationById(fromStationId));
        model.addAttribute("toStation", stationService.getStationById(toStationId));
        model.addAttribute("fromStationId", fromStationId);
        model.addAttribute("toStationId", toStationId);
    }
}
