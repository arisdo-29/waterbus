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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
                        Model model, RedirectAttributes flash) {
        if (fromStationId == null || toStationId == null) {
            flash.addFlashAttribute("errorMessage", "Vui lòng chọn bến đi và bến đến trước khi chọn ghế.");
            return "redirect:/trips";
        }
        BookingTripDTO trip = tripService.getBookableTrip(id, fromStationId, toStationId);
        if (trip == null) {
            flash.addFlashAttribute("errorMessage", "Chuyến không còn mở bán hoặc chặng đã chọn không hợp lệ. Vui lòng tìm lại chuyến.");
            return "redirect:/trips";
        }
        model.addAttribute("trip", trip);
        model.addAttribute("seats", tripService.getSeatMap(id));
        model.addAttribute("ticketTypes", ticketTypeService.getActiveTicketTypeDTOs());
        model.addAttribute("fromStationId", fromStationId);
        model.addAttribute("toStationId", toStationId);
        model.addAttribute("fromStation", stationService.getStationById(fromStationId));
        model.addAttribute("toStation", stationService.getStationById(toStationId));
        return "trip/seats";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleInvalidParameter(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Đường dẫn hoặc tham số tìm chuyến không hợp lệ.");
        return "redirect:/trips";
    }
}
