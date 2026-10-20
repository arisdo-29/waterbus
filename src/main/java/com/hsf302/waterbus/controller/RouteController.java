package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.RouteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.hsf302.waterbus.dto.RouteDTO;
import com.hsf302.waterbus.dto.StationDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public String index(Model model) {
        List<RouteDTO> routes = routeService.getActiveRouteViews();
        Map<Integer, List<StationDTO>> stopsByRoute = new LinkedHashMap<>();
        for (RouteDTO route : routes) {
            stopsByRoute.put(route.id(), routeService.getStationViewsOfRoute(route.id()));
        }
        model.addAttribute("routes", routes);
        model.addAttribute("stopsByRoute", stopsByRoute);
        return "route/index";
    }
}
