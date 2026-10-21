package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.BookingDTO;
import com.hsf302.waterbus.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE4 - Đặt vé & Thanh toán. Mọi URL của controller này bắt đầu bằng /booking
// View đặt trong templates/booking/ (giao diện: Khuyên)
@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String index() {
        // TODO BE4: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "booking/index";
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
}
