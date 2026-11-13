package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.RouteForm;
import com.hsf302.waterbus.service.RouteService;
import com.hsf302.waterbus.service.StationService;
import com.hsf302.waterbus.service.TripService;
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
    private final TripService tripService;

    public AdminRouteController(RouteService routeService, StationService stationService,
                                TripService tripService) {
        this.routeService = routeService;
        this.stationService = stationService;
        this.tripService = tripService;
    }

    @InitBinder("route")
    void bindRoute(WebDataBinder binder) {
        binder.setAllowedFields("id", "code", "name", "description", "isActive", "stationIds", "stationIds[*]",
                "minutesFromStart", "minutesFromStart[*]");
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
    public String edit(@PathVariable int id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("route", routeService.getRouteForEdit(id));
            assertRouteEditable(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/routes";
        }
        return form(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("route") RouteForm form, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() != null) {
                    assertRouteEditable(form.getId());
                }
                routeService.saveRoute(form);
                flash.addFlashAttribute("successMessage", "Đã lưu tuyến và thứ tự bến.");
                return "redirect:/admin/routes";
            } catch (IllegalArgumentException | IllegalStateException ex) {
                errors.reject("route.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("route.conflict", "Không thể lưu tuyến. Mã có thể đã tồn tại hoặc dữ liệu vừa thay đổi. Vui lòng kiểm tra và thử lại.");
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
            long active = tripService.countActiveTripsOfRoute(id);
            if (active > 0) {
                throw new IllegalArgumentException("Tuyến còn " + active
                        + " chuyến đang chạy hoặc sắp khởi hành. Vui lòng chờ hoàn tất hoặc huỷ các chuyến này trước khi ngừng tuyến.");
            }
            routeService.hideRoute(id);
            flash.addFlashAttribute("successMessage", "Đã ngừng hoạt động tuyến. Chuyến và dữ liệu lịch sử được giữ nguyên.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/routes";
    }

    private void assertRouteEditable(int routeId) {
        long active = tripService.countActiveTripsOfRoute(routeId);
        if (active > 0) {
            throw new IllegalStateException("Tuyến còn " + active
                    + " chuyến đang chạy hoặc sắp khởi hành. Chỉ được sửa tuyến sau khi các chuyến này hoàn tất hoặc bị huỷ.");
        }
    }

    private String form(Model model) {
        model.addAttribute("stations", stationService.getStations());
        return "admin/route/form";
    }
}
