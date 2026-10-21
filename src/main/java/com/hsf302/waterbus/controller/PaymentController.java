package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.service.BookingService;
import com.hsf302.waterbus.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE4 - Đặt vé & Thanh toán. Mọi URL của controller này bắt đầu bằng /payment
// View đặt trong templates/payment/ (giao diện: Khuyên)
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService; // cùng module BE4: đọc thông tin đơn

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    // GET /payment/BK12345678 -> trang thanh toán của đơn (làm giống detail() trong BookingController)
    @GetMapping("/{bookingCode}")
    public String index(@PathVariable String bookingCode, Model model, RedirectAttributes flash) {
        BookingDTO booking = bookingService.getBookingDetail(bookingCode);
        if (booking == null) {
            // Sai mã đơn -> về trang chủ và báo lỗi
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        // Tên attribute đúng theo docs/PAGES.md: booking, trip, tickets
        model.addAttribute("booking", booking);
        model.addAttribute("trip", bookingService.getTripOfBooking(booking.getId()));
        model.addAttribute("tickets", bookingService.getTicketsOfBooking(booking.getId()));
        return "payment/index";
    }

    // POST /payment/BK12345678/mock -> nút "Thanh toán" trên trang thanh toán
    @PostMapping("/{bookingCode}/mock")
    public String payMock(@PathVariable String bookingCode, RedirectAttributes flash) {
        Booking booking = bookingService.getBookingByCode(bookingCode);
        if (booking == null) {
            flash.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt vé có mã " + bookingCode + ".");
            return "redirect:/";
        }
        try {
            paymentService.payMock(booking.getId());
            // Thành công -> sang trang xem vé, báo xanh qua fragment alerts
            flash.addFlashAttribute("successMessage", "Thanh toán thành công! Vé của bạn đã sẵn sàng.");
            return "redirect:/booking/" + booking.getBookingCode();
        } catch (IllegalArgumentException ex) {
            // Đơn đã trả tiền / hết hạn -> quay lại trang thanh toán, báo đỏ
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payment/" + booking.getBookingCode();
        }
    }
}
