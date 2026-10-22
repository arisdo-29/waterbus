package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE4 - Đặt vé & Thanh toán. Controller riêng vì URL /my-bookings không bắt đầu bằng /booking
// View: templates/booking/history.html (giao diện: Khuyên)
@Controller
@RequestMapping("/my-bookings")
public class MyBookingController {

    private final BookingService bookingService;

    public MyBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // GET /my-bookings -> lịch sử đặt vé của tài khoản đang đăng nhập
    @GetMapping
    public String history(HttpSession session, Model model) {
        // Lấy tài khoản từ session (làm giống AccountController.viewProfile())
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount == null) {
            return "redirect:/auth/login"; // chưa đăng nhập -> sang trang đăng nhập
        }
        // Tên attribute đúng theo docs/PAGES.md: bookings
        model.addAttribute("bookings", bookingService.getBookingsOfAccount(loginAccount.getId()));
        return "booking/history";
    }
}
