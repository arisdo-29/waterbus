package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final StationService stationService;

    public HomeController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("stations", stationService.getActiveStations());
        return "index";
    }
}
