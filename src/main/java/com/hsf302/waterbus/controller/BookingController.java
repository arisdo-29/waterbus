package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.StationService;
import com.hsf302.waterbus.service.TicketTypeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
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

    // GET /booking: không có trang riêng, chuyển sang lịch sử đặt vé của tài khoản
    @GetMapping
    public String index() {
        return "redirect:/my-bookings";
    }

    // POST /booking/checkout: form ở trang chọn ghế (trip/seats) gửi lên -> trang xem lại đơn, CHƯA lưu gì vào DB
    @PostMapping("/checkout")
    public String checkout(@RequestParam int tripId,
                           @RequestParam(required = false) List<Integer> seatIds, // nhiều checkbox cùng tên
                           @RequestParam int ticketTypeId,
                           @RequestParam int fromStationId,
                           @RequestParam int toStationId,
                           Model model, RedirectAttributes flash) {
        // Chuyến hoặc loại vé không tồn tại (sửa tay form) -> về trang tìm chuyến
        if (!tripAndTicketTypeExist(tripId, ticketTypeId)) {
            flash.addFlashAttribute("errorMessage", "Chuyến hoặc loại vé không hợp lệ. Vui lòng chọn lại.");
            return "redirect:/trips";
        }
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
    // @Valid: Spring kiểm tra các annotation trong BookingForm, kết quả nằm trong BindingResult "errors".
    // @ModelAttribute("bookingForm"): form + lỗi tự có trong model với tên "bookingForm" để template dùng th:errors.
    @PostMapping
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form, BindingResult errors,
                         HttpSession session, Model model, RedirectAttributes flash) {
        // Thiếu chuyến/loại vé/bến (form bị sửa tay) -> không vẽ lại được trang checkout -> báo lỗi 400
        if (form.getTripId() == null || form.getTicketTypeId() == null
                || form.getFromStationId() == null || form.getToStationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin chuyến, loại vé hoặc bến.");
        }

        if (!tripAndTicketTypeExist(form.getTripId(), form.getTicketTypeId())) {
            flash.addFlashAttribute("errorMessage", "Chuyến hoặc loại vé không hợp lệ. Vui lòng chọn lại.");
            return "redirect:/trips";
        }
        if (errors.hasErrors()) {
            // Nhập sai (trống tên, SĐT sai...) -> chưa gọi service, báo lỗi
            model.addAttribute("errorMessage", errorText(errors));
        } else {
            // Lấy tài khoản đang đăng nhập (Tân lưu vào session tên "loginAccount" khi đăng nhập)
            AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
            // Đã đăng nhập -> id tài khoản; chưa đăng nhập -> null (khách vãng lai)
            Integer accountId = (loginAccount == null) ? null : loginAccount.getId();
            try {
                Booking booking = bookingService.createBooking(form.getTripId(), form.getSeatIds(),
                        form.getTicketTypeId(), form.getFromStationId(), form.getToStationId(), form.getPromoCode(),
                        accountId, form.getContactName(), form.getContactPhone(), form.getContactEmail());
                // Thành công -> sang trang thanh toán. Dùng redirect để bấm F5 không tạo đơn lần 2
                return "redirect:/payment/" + booking.getBookingCode();
            } catch (IllegalArgumentException ex) {
                // Lỗi nghiệp vụ (ghế vừa bị đặt, mã KM sai...) -> báo lỗi
                model.addAttribute("errorMessage", ex.getMessage());
            }
        }
        // Có lỗi -> hiện lại trang checkout, KHÔNG redirect để giữ dữ liệu đã nhập.
        // Họ tên/SĐT/email/mã KM tự giữ vì template đọc ${param...} của chính request này.
        fillCheckoutModel(model, form.getTripId(), form.getSeatIds(), form.getTicketTypeId(),
                form.getFromStationId(), form.getToStationId());
        return "booking/checkout";
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

    // Chuyến còn mở bán và loại vé có tồn tại thì mới vẽ được trang checkout
    private boolean tripAndTicketTypeExist(int tripId, int ticketTypeId) {
        return bookingService.getTripInfo(tripId) != null && ticketTypeService.getTicketTypeById(ticketTypeId) != null;
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
        // Template dùng th:object="${bookingForm}" -> lúc mới vào checkout (chưa gửi form) cũng phải có object rỗng
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingForm());
        }
    }

    // Gộp các lỗi validate thành 1 câu cho alert đỏ (errorMessage) ở đầu form
    private String errorText(BindingResult errors) {
        String text = "";
        for (ObjectError error : errors.getAllErrors()) {
            String message = error.getDefaultMessage();
            // Lỗi đổi kiểu (VD seatIds=abc) có message tiếng Anh khó hiểu -> thay bằng câu dễ hiểu
            if (error instanceof FieldError field && field.isBindingFailure()) {
                message = "Dữ liệu gửi lên không hợp lệ.";
            }
            text = text + message + " ";
        }
        return text.trim();
    }
}
