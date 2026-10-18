package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE4 - Đặt vé & Thanh toán. Mọi URL của controller này bắt đầu bằng /payment
// View đặt trong templates/payment/ (giao diện: Khuyên)
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public String index() {
        // TODO BE4: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "payment/index";
    }
}
