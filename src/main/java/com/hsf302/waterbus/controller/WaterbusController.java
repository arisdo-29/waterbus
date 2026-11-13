package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.WaterbusService;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.dto.WaterbusDTO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/admin/waterbuses")
public class WaterbusController {
    private static final DateTimeFormatter DATE_TIME_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

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

    @GetMapping("/{id}/status")
    public String statusPlan(@PathVariable int id, @RequestParam String status,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                             LocalDateTime startsAt,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                             LocalDateTime endsAt,
                             @RequestParam(defaultValue = "false") boolean preview,
                             Model model, RedirectAttributes flash) {
        WaterbusDTO waterbus = waterbusService.getWaterbusViews().stream()
                .filter(candidate -> candidate.id() == id).findFirst().orElse(null);
        if (waterbus == null || !List.of("ACTIVE", "MAINTENANCE", "INACTIVE").contains(status)) {
            flash.addFlashAttribute("errorMessage", "Mã tàu hoặc trạng thái không hợp lệ.");
            return "redirect:/admin/waterbuses";
        }
        model.addAttribute("waterbus", waterbus);
        model.addAttribute("targetStatus", status);
        model.addAttribute("startsAt", startsAt == null ? "" : startsAt.format(DATE_TIME_LOCAL));
        model.addAttribute("endsAt", endsAt == null ? "" : endsAt.format(DATE_TIME_LOCAL));
        model.addAttribute("minStartsAt", LocalDateTime.now().plusMinutes(1).format(DATE_TIME_LOCAL));
        model.addAttribute("previewReady", false);
        model.addAttribute("affectedTrips", List.of());
        if (preview || "ACTIVE".equals(status)) {
            try {
                var affectedTrips = waterbusService.previewStatusChange(id, status, startsAt, endsAt);
                model.addAttribute("affectedTrips", affectedTrips);
                model.addAttribute("previewReady", true);
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorMessage", ex.getMessage());
            }
        }
        return "admin/waterbus/status";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable int id, @RequestParam String status,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                               LocalDateTime startsAt,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                               LocalDateTime endsAt,
                               @RequestParam(required = false) List<Integer> tripIds,
                               RedirectAttributes flash) {
        try {
            int cancelled = waterbusService.scheduleStatusChange(id, status, startsAt, endsAt, tripIds);
            String message = "ACTIVE".equals(status) ? "Tàu đã hoạt động trở lại."
                    : "Đã lên lịch ngưng tàu và tự động huỷ " + cancelled + " chuyến."
                    + (cancelled > 0 ? " Các đơn đã thanh toán được chuyển sang danh sách chờ hoàn tiền." : "");
            flash.addFlashAttribute("successMessage", message);
            return "redirect:/admin/waterbuses";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            flash.addAttribute("id", id);
            flash.addAttribute("status", status);
            if (startsAt != null) flash.addAttribute("startsAt", startsAt.format(DATE_TIME_LOCAL));
            if (endsAt != null) flash.addAttribute("endsAt", endsAt.format(DATE_TIME_LOCAL));
            flash.addAttribute("preview", true);
            return "redirect:/admin/waterbuses/{id}/status";
        }
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handleInvalidParameter(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Mã tàu hoặc trạng thái không hợp lệ.");
        return "redirect:/admin/waterbuses";
    }
}
