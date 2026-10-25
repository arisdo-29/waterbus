package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.WaterbusService;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE2 - Tàu, Ghế & Chuyến. Mọi URL của controller này bắt đầu bằng /admin/waterbuses
// View đặt trong templates/admin/waterbus/ (giao diện: Trí)
@Controller
@RequestMapping("/admin/waterbuses")
public class WaterbusController {

    private final WaterbusService waterbusService;

    public WaterbusController(WaterbusService waterbusService) {
        this.waterbusService = waterbusService;
    }

    @InitBinder("waterbus")
    void bindWaterbus(WebDataBinder binder) {
        binder.setAllowedFields("id", "code", "name", "capacity", "status");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("waterbuses", waterbusService.getWaterbuses());
        return "admin/waterbus/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("waterbus", new AdminWaterbusForm());
        return "admin/waterbus/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable int id, Model model) {
        try {
            model.addAttribute("waterbus", waterbusService.getWaterbusForEdit(id));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
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
}
