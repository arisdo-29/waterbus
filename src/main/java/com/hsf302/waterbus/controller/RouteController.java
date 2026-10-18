package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.RouteService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE1 - Tuyến & Bến. Mọi URL của controller này bắt đầu bằng /routes
// View đặt trong templates/route/ (giao diện: Khuyên)
@Controller
@RequestMapping("/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public String index() {
        // TODO BE1: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "route/index";
    }
}
