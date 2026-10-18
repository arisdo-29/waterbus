package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.WaterbusService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE2 - Tàu, Ghế & Chuyến. Mọi URL của controller này bắt đầu bằng /admin/waterbuses
// View đặt trong templates/admin/waterbus/ (giao diện: Trí)
@Controller
@RequestMapping("/admin/waterbuses")
public class WaterbusController {

    private final WaterbusService waterbusService;

    public WaterbusController(WaterbusService waterbusService) {
        this.waterbusService = waterbusService;
    }

    @GetMapping
    public String index() {
        // TODO BE2: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "admin/waterbus/index";
    }
}
