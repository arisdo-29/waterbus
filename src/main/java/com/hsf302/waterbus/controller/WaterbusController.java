package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.WaterbusService;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/waterbuses")
public class WaterbusController {

    private final WaterbusService waterbusService;

    public WaterbusController(WaterbusService waterbusService) {
        this.waterbusService = waterbusService;
    }

    @InitBinder("waterbus")
    void bindWaterbus(WebDataBinder binder) {
        binder.setAllowedFields("code", "name", "capacity", "status");
    }

    @GetMapping
    public String list(Model model) {
        var waterbuses = waterbusService.getWaterbusViews();
        model.addAttribute("waterbuses", waterbuses);
        Map<Integer, Long> scheduledTripCounts = waterbuses.stream().collect(Collectors.toMap(
                waterbus -> waterbus.id(), waterbus -> waterbusService.countUpcomingScheduledTrips(waterbus.id())));
        model.addAttribute("scheduledTripCounts", scheduledTripCounts);
        return "admin/waterbus/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("waterbus", new AdminWaterbusForm());
        return "admin/waterbus/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("waterbus") AdminWaterbusForm form, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                waterbusService.saveWaterbus(form);
                flash.addFlashAttribute("successMessage", "Đã lưu thông tin tàu.");
                return "redirect:/admin/waterbuses";
            } catch (IllegalArgumentException ex) {
                errors.reject("waterbus.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("waterbus.conflict", "Không thể lưu tàu. Mã có thể đã tồn tại hoặc dữ liệu vừa thay đổi.");
            }
        }
        model.addAttribute("errorMessage", errors.getAllErrors().stream()
                .map(error -> error.getDefaultMessage()).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(" ")));
        return "admin/waterbus/form";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable int id, @RequestParam String status, RedirectAttributes flash) {
        try {
            waterbusService.changeStatus(id, status);
            flash.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tàu.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/waterbuses";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handleInvalidParameter(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Mã tàu hoặc trạng thái không hợp lệ.");
        return "redirect:/admin/waterbuses";
    }
}
