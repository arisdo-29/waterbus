package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
