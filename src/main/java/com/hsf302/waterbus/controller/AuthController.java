package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. Mọi URL của controller này bắt đầu bằng /auth
// View đặt trong templates/auth/
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String index() {
        // TODO BE3: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "auth/index";
    }
}
