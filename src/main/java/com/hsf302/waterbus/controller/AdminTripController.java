package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.AdminTripForm;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.TripService;
import com.hsf302.waterbus.service.WaterbusService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/trips")
public class AdminTripController {
    private final TripService tripService;
    private final RouteService routeService;
    private final WaterbusService waterbusService;

    public AdminTripController(TripService tripService, RouteService routeService, WaterbusService waterbusService) {
        this.tripService = tripService;
        this.routeService = routeService;
        this.waterbusService = waterbusService;
    }

    @InitBinder("trip")
    void bindTrip(WebDataBinder binder) {
        binder.setAllowedFields("routeId", "waterbusId", "departureTime", "status");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("trips", tripService.getTripsForAdmin());
        return "admin/trip/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("trip", new AdminTripForm());
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("trip") AdminTripForm trip, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                tripService.createAdminTrip(trip);
                flash.addFlashAttribute("successMessage", "Đã tạo chuyến tàu.");
                return "redirect:/admin/trips";
            } catch (IllegalArgumentException ex) {
                errors.reject("trip.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("trip.conflict", "Không thể tạo chuyến. Mã chuyến có thể bị trùng hoặc dữ liệu vừa thay đổi.");
            }
        }
        model.addAttribute("errorMessage", errors.getAllErrors().stream()
                .map(error -> error.getDefaultMessage()).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" ")));
        return form(model);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable int id, RedirectAttributes flash) {
        try {
            tripService.cancelTrip(id);
            flash.addFlashAttribute("successMessage", "Đã huỷ chuyến tàu.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/trips";
    }

    private String form(Model model) {
        model.addAttribute("routes", routeService.getActiveRoutes());
        model.addAttribute("waterbuses", waterbusService.getWaterbuses().stream()
                .filter(waterbus -> "ACTIVE".equals(waterbus.getStatus())).toList());
        return "admin/trip/form";
    }
}
