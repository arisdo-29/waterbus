package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.RevenueReportDTO;
import com.hsf302.waterbus.service.RevenueService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/revenue")
public class AdminRevenueController {
    private final RevenueService revenueService;

    public AdminRevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        Model model) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) {
            model.addAttribute("errorMessage", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            end = LocalDate.now();
            start = end.minusDays(6);
        }
        RevenueReportDTO report = revenueService.getReport(start, end);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("summary", report.summary());
        model.addAttribute("dailyRevenue", report.dailyRevenue());
        model.addAttribute("byRoute", report.byRoute());
        model.addAttribute("byTicketType", report.byTicketType());
        model.addAttribute("byMethod", report.byMethod());
        model.addAttribute("byPromotion", report.byPromotion());
        return "admin/revenue/index";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleInvalidDate(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Ngày báo cáo không đúng định dạng.");
        return "redirect:/admin/revenue";
    }
}
