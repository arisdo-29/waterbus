package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.service.StationService;
import com.hsf302.waterbus.service.TicketTypeService;
import com.hsf302.waterbus.service.TripService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Module: BE2 - Tàu, Ghế & Chuyến. Mọi URL của controller này bắt đầu bằng /trips
// View đặt trong templates/trip/ (giao diện: Trí)
@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final StationService stationService;
    private final TicketTypeService ticketTypeService;

    public TripController(TripService tripService, StationService stationService,
                          TicketTypeService ticketTypeService) {
        this.tripService = tripService;
        this.stationService = stationService;
        this.ticketTypeService = ticketTypeService;
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

    @GetMapping("/{id}/seats")
    public String seats(@PathVariable int id,
                        @RequestParam(required = false) Integer fromStationId,
                        @RequestParam(required = false) Integer toStationId,
                        Model model) {
        BookingTripDTO trip = tripService.getBookableTrip(id);
        if (trip == null) {
            return "redirect:/trips";
        }
        model.addAttribute("trip", trip);
        model.addAttribute("seats", tripService.getSeatMap(id));
        model.addAttribute("ticketTypes", ticketTypeService.getActiveTicketTypes());
        model.addAttribute("fromStationId", fromStationId);
        model.addAttribute("toStationId", toStationId);
        model.addAttribute("fromStation", fromStationId == null ? null : stationService.getStationById(fromStationId));
        model.addAttribute("toStation", toStationId == null ? null : stationService.getStationById(toStationId));
        return "trip/seats";
    }
}
