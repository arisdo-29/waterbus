package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my-bookings")
public class MyBookingController {

    private final BookingService bookingService;

    public MyBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String history(HttpSession session, Model model) {
        AccountDTO loginAccount = (AccountDTO) session.getAttribute("loginAccount");
        if (loginAccount == null) {
            session.setAttribute("redirectAfterLogin", "/my-bookings");
            return "redirect:/auth/login";
        }
        model.addAttribute("bookings", bookingService.getBookingsOfAccount(loginAccount.getId()));
        return "booking/history";
    }
}
