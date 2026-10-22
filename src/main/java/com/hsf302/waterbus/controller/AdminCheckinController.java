package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.service.TicketService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE3 (Tân) - Soát vé (Staff Check-in) theo M4
// Endpoint nằm dưới /admin/** để được bảo vệ bởi AdminInterceptor (chỉ STAFF và ADMIN được truy cập)
@Controller
@RequestMapping("/admin/checkin")
public class AdminCheckinController {

    private final TicketService ticketService;

    // Constructor Injection
    public AdminCheckinController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public String index(@RequestParam(name = "ticketCode", required = false) String ticketCode, Model model) {
        if (ticketCode != null && !ticketCode.trim().isEmpty()) {
            try {
                TicketCheckinDTO ticket = ticketService.getTicketInfo(ticketCode.trim());
                model.addAttribute("previewTicket", ticket);
                model.addAttribute("ticketCode", ticketCode.trim());
            } catch (Exception e) {
                model.addAttribute("errorMessage", e.getMessage());
                model.addAttribute("ticketCode", ticketCode.trim());
            }
        }
        return "admin/checkin/index";
    }

    @PostMapping
    public String checkIn(@RequestParam("ticketCode") String ticketCode, RedirectAttributes redirectAttributes) {
        try {
            TicketCheckinDTO checkedIn = ticketService.checkIn(ticketCode);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Soát vé thành công! Vé [" + checkedIn.getTicketCode() + "] đã được xác nhận hợp lệ lên tàu.");
            redirectAttributes.addFlashAttribute("checkedInTicket", checkedIn);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("ticketCode", ticketCode);
        }
        return "redirect:/admin/checkin";
    }
}
