package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE2 - Tàu, Ghế & Chuyến. Mọi URL của controller này bắt đầu bằng /trips
// View đặt trong templates/trip/
@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public String index() {
        // TODO BE2: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "trip/index";
    }
}
