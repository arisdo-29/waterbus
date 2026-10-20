package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.StationService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/routes")
public class AdminRouteController {
    private final RouteService routeService;
    private final StationService stationService;

    public AdminRouteController(RouteService routeService, StationService stationService) {
        this.routeService = routeService;
        this.stationService = stationService;
    }

    @InitBinder("route")
    void bindRoute(WebDataBinder binder) {
        binder.setAllowedFields("id", "code", "name", "description", "isActive", "stationIds", "stationIds[*]");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("routes", routeService.getRoutesForAdmin());
        return "admin/route/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("route", new RouteForm());
        return form(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable int id, Model model) {
        try {
            model.addAttribute("route", routeService.getRouteForEdit(id));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("route") RouteForm form, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                routeService.saveRoute(form);
                flash.addFlashAttribute("successMessage", "Đã lưu tuyến và thứ tự bến.");
                return "redirect:/admin/routes";
            } catch (IllegalArgumentException ex) {
                errors.reject("route.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("route.conflict", "Không thể lưu tuyến. Mã có thể đã tồn tại hoặc dữ liệu vừa thay đổi; vui lòng kiểm tra và thử lại.");
            }
        }
        model.addAttribute("errorMessage", errors.getAllErrors().stream()
                .map(e -> e instanceof FieldError field && field.isBindingFailure()
                        ? "Giá trị nhập không hợp lệ. Vui lòng kiểm tra mã định danh, bến và trạng thái tuyến."
                        : e.getDefaultMessage()).collect(java.util.stream.Collectors.joining(" ")));
        return form(model);
    }

    @PostMapping("/{id}/delete")
    public String hide(@PathVariable int id, RedirectAttributes flash) {
        try {
            routeService.hideRoute(id);
            flash.addFlashAttribute("successMessage", "Đã ngừng hoạt động tuyến; không xoá chuyến hoặc dữ liệu lịch sử.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/routes";
    }

    private String form(Model model) {
        // Include inactive stations so legacy/inactive routes and invalid submissions retain their selection.
        model.addAttribute("stations", stationService.getStations());
        return "admin/route/form";
    }
}
