package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.StationForm;
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
@RequestMapping("/admin/stations")
public class AdminStationController {
    private final StationService stationService;

    public AdminStationController(StationService stationService) {
        this.stationService = stationService;
    }

    @InitBinder("station")
    void bindStation(WebDataBinder binder) {
        binder.setAllowedFields("id", "code", "name", "address", "isActive");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("stations", stationService.getStations());
        return "admin/station/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("station", new StationForm());
        return "admin/station/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable int id, Model model) {
        try {
            model.addAttribute("station", stationService.getStationForEdit(id));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        return "admin/station/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("station") StationForm form, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                stationService.saveStation(form);
                flash.addFlashAttribute("successMessage", "Đã lưu thông tin bến.");
                return "redirect:/admin/stations";
            } catch (IllegalArgumentException ex) {
                errors.reject("station.invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("station.conflict", "Không thể lưu bến. Mã có thể đã tồn tại; vui lòng kiểm tra và thử lại.");
            }
        }
        model.addAttribute("errorMessage", errors.getAllErrors().stream()
                .map(e -> e instanceof FieldError field && field.isBindingFailure()
                        ? "Giá trị nhập không hợp lệ. Vui lòng kiểm tra mã định danh và trạng thái bến."
                        : e.getDefaultMessage()).collect(java.util.stream.Collectors.joining(" ")));
        return "admin/station/form";
    }

    @PostMapping("/{id}/delete")
    public String hide(@PathVariable int id, RedirectAttributes flash) {
        try {
            stationService.hideStation(id);
            flash.addFlashAttribute("successMessage", "Đã ẩn bến; dữ liệu tuyến, chuyến và vé được giữ nguyên.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/stations";
    }
}
