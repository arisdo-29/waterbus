package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.StationService;
import com.hsf302.waterbus.service.TripService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Module: BE2 - Tàu, Ghế & Chuyến. Mọi URL của controller này bắt đầu bằng /trips
// View đặt trong templates/trip/ (giao diện: Trí)
@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final StationService stationService;

    public TripController(TripService tripService, StationService stationService) {
        this.tripService = tripService;
        this.stationService = stationService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) Integer fromStationId,
                        @RequestParam(required = false) Integer toStationId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        model.addAttribute("stations", stationService.getActiveStations());
        model.addAttribute("fromStationId", fromStationId);
        model.addAttribute("toStationId", toStationId);
        model.addAttribute("date", date);
        if (fromStationId != null && toStationId != null && date != null) {
            model.addAttribute("trips", tripService.searchTripResults(fromStationId, toStationId, date));
        }
        return "trip/index";
    }
}
