package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.dto.BookingForm;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PaymentService;
import com.hsf302.waterbus.service.RefundService;
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

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    private final StationService stationService;
    private final TicketTypeService ticketTypeService;
    private final RefundService refundService;
    private final PaymentService paymentService;
    private final BookingAccessHelper bookingAccess;

    public BookingController(BookingService bookingService, StationService stationService,
                             TicketTypeService ticketTypeService, PaymentService paymentService,
                             RefundService refundService, BookingAccessHelper bookingAccess) {
        this.bookingService = bookingService;
        this.stationService = stationService;
        this.ticketTypeService = ticketTypeService;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.bookingAccess = bookingAccess;
    }

    @GetMapping
    public String index() {
        return "redirect:/my-bookings";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam int tripId,
                           @RequestParam(required = false) List<Integer> seatIds,
                           @RequestParam int ticketTypeId,
                           @RequestParam(required = false) Integer fromStationId,
                           @RequestParam(required = false) Integer toStationId,
                           Model model, RedirectAttributes flash) {
        if (fromStationId == null || toStationId == null) {
            flash.addFlashAttribute("errorMessage", "Vui lòng chọn bến đi và bến đến trước khi đặt vé.");
            return "redirect:/trips";
        }
        if (!tripAndTicketTypeExist(tripId, fromStationId, toStationId, ticketTypeId)) {
            flash.addFlashAttribute("errorMessage", "Chuyến hoặc loại vé không hợp lệ. Vui lòng chọn lại.");
            return "redirect:/trips";
        }
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
        fillCheckoutModel(model, tripId, seatIds, ticketTypeId, fromStationId, toStationId);
        return "booking/checkout";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form, BindingResult errors,
                         HttpSession session, Model model, RedirectAttributes flash) {
        if (form.getTripId() == null || form.getTicketTypeId() == null
                || form.getFromStationId() == null || form.getToStationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin chuyến, loại vé hoặc bến.");
        }

        if (!tripAndTicketTypeExist(form.getTripId(), form.getFromStationId(), form.getToStationId(),
                form.getTicketTypeId())) {
            flash.addFlashAttribute("errorMessage", "Chuyến hoặc loại vé không hợp lệ. Vui lòng chọn lại.");
            return "redirect:/trips";
        }
        bookingService.validatePassengers(form, errors);
        if (errors.hasErrors()) {
            model.addAttribute("errorMessage", errorText(errors));
        } else {
            AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
            Integer accountId = (loginAccount == null) ? null : loginAccount.getId();
            try {
                BookingDTO booking = bookingService.createBooking(form, accountId);
                if (accountId == null) {
                    bookingAccess.allowInThisBrowser(session, booking.getBookingCode());
                }
                if ("PAID".equals(booking.getStatus())) {
                    flash.addFlashAttribute("successMessage", "Đặt vé thành công! Vé của bạn đã sẵn sàng.");
                    return "redirect:/booking/" + booking.getBookingCode();
                }
                return "redirect:/payment/" + booking.getBookingCode();
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
            }
        }
        fillCheckoutModel(model, form.getTripId(), form.getSeatIds(), form.getTicketTypeId(),
                form.getFromStationId(), form.getToStationId());
        return "booking/checkout";
    }

    @GetMapping("/{bookingCode}")
    public String detail(@PathVariable String bookingCode, HttpSession session, Model model,
                         RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        String blocked = bookingAccess.redirectIfCannotView(bookingCode, "/booking/" + bookingCode, session, flash);
        if (blocked != null) {
            return blocked;
        }
        model.addAttribute("booking", booking);
        model.addAttribute("trip", bookingService.getTripOfBooking(booking.getId()));
        model.addAttribute("tickets", bookingService.getTicketsOfBooking(booking.getId()));
        model.addAttribute("refund", refundService.getRefundViewOfBooking(booking.getId()));
        model.addAttribute("tripCancelReason", bookingService.getTripCancelReason(booking.getId()));
        model.addAttribute("payment", paymentService.getPaymentOfBooking(booking.getId()));
        return "booking/detail";
    }

    @PostMapping("/{bookingCode}/cancel")
    public String cancel(@PathVariable String bookingCode, HttpSession session, RedirectAttributes flash) {
        String blocked = bookingAccess.redirectIfCannotView(bookingCode, "/booking/" + bookingCode, session, flash);
        if (blocked != null) {
            return blocked;
        }
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        Integer accountId = (loginAccount == null) ? null : loginAccount.getId();
        try {
            bookingService.cancelBooking(bookingCode, accountId);
            flash.addFlashAttribute("successMessage", "Đã huỷ đơn.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/booking/{bookingCode}";
    }

    @GetMapping("/{bookingCode}/verify")
    public String verifyForm(@PathVariable String bookingCode, Model model) {
        model.addAttribute("bookingCode", bookingCode);
        return "booking/verify";
    }

    @PostMapping("/{bookingCode}/verify")
    public String verify(@PathVariable String bookingCode, @RequestParam(required = false) String contactPhone,
                         HttpSession session, RedirectAttributes flash) {
        if (!bookingService.verifyContactPhone(bookingCode, contactPhone)) {
            flash.addFlashAttribute("errorMessage", "Số điện thoại không khớp với đơn đặt vé này.");
            return "redirect:/booking/" + bookingCode + "/verify";
        }
        bookingAccess.allowInThisBrowser(session, bookingCode);
        return "redirect:/booking/" + bookingCode;
    }

    private boolean tripAndTicketTypeExist(int tripId, int fromStationId, int toStationId, int ticketTypeId) {
        return bookingService.getTripInfo(tripId, fromStationId, toStationId) != null
                && ticketTypeService.getTicketTypeById(ticketTypeId) != null;
    }

    private void fillCheckoutModel(Model model, int tripId, List<Integer> seatIds, int ticketTypeId,
                                   int fromStationId, int toStationId) {
        List<SeatDTO> seats = bookingService.getSelectedSeats(tripId, seatIds);
        model.addAttribute("trip", bookingService.getTripInfo(tripId, fromStationId, toStationId));
        model.addAttribute("seats", seats);
        model.addAttribute("ticketType", ticketTypeService.getTicketTypeById(ticketTypeId));
        model.addAttribute("subtotal", bookingService.calculateSubtotal(ticketTypeId, seats.size()));
        model.addAttribute("fromStation", stationService.getStationById(fromStationId));
        model.addAttribute("toStation", stationService.getStationById(toStationId));
        model.addAttribute("fromStationId", fromStationId);
        model.addAttribute("toStationId", toStationId);
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingForm());
        }
    }

    private String errorText(BindingResult errors) {
        boolean hasErrorUnderField = false;
        String text = "";
        for (ObjectError error : errors.getAllErrors()) {
            if (error instanceof FieldError field && isShownUnderField(field.getField())) {
                hasErrorUnderField = true;
                continue;
            }
            String message = error.getDefaultMessage();
            if (error instanceof FieldError field && field.isBindingFailure()) {
                message = "Dữ liệu gửi lên không hợp lệ.";
            }
            if (!text.contains(message)) {
                text = text + message + " ";
            }
        }
        if (hasErrorUnderField) {
            text = "Vui lòng kiểm tra các ô được đánh dấu. " + text;
        }
        return text.trim();
    }

    private boolean isShownUnderField(String fieldName) {
        return fieldName.equals("contactName") || fieldName.equals("contactPhone")
                || fieldName.equals("contactEmail") || fieldName.equals("promoCode")
                || fieldName.equals("acceptPolicy")
                || fieldName.startsWith("passengerNames") || fieldName.startsWith("birthYears");
    }
}
