package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE1 - Tuyến & Bến. Mọi URL của controller này bắt đầu bằng /stations
@Controller
@RequestMapping("/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("stations", stationService.getStations());
        return "station/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model model) {
        StationDTO station = stationService.getStationById(id);
        if (station == null) {
            return "redirect:/stations";
        }
        model.addAttribute("station", station);
        return "station/view";
    }
}
