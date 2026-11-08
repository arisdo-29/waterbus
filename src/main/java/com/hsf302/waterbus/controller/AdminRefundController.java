package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.RefundService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/refunds")
public class AdminRefundController {
    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        String filter = status == null ? "" : status.trim().toUpperCase();
        try {
            model.addAttribute("refunds", refundService.getRefunds(filter));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            filter = "";
            model.addAttribute("refunds", refundService.getRefunds(filter));
        }
        model.addAttribute("statusFilter", filter);
        return "admin/refund/list";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable int id, @RequestParam String transactionRef,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime refundedAt,
                           @RequestParam(required = false) String note, RedirectAttributes flash) {
        try {
            refundService.markRefunded(id, transactionRef, refundedAt, note);
            flash.addFlashAttribute("successMessage", "Đã ghi nhận hoàn tiền.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}
