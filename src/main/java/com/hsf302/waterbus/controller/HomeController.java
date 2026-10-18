package com.hsf302.waterbus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// FILE CHUNG - Leader phụ trách. Mỗi module có Controller riêng, đừng thêm mapping của module vào đây.
@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
