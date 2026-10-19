package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.TicketTypeDTO;
import com.hsf302.waterbus.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. Mọi URL của controller này bắt đầu bằng /admin/ticket-types
// View đặt trong templates/admin/tickettype/ (giao diện: Tân)
@Controller
@RequestMapping("/admin/ticket-types")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
        return "admin/tickettype/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        if (!model.containsAttribute("ticketType")) {
            model.addAttribute("ticketType", new TicketTypeDTO());
        }
        return "admin/tickettype/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        TicketTypeDTO dto = ticketTypeService.getTicketTypeById(id);
        if (dto == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy loại vé.");
            return "redirect:/admin/ticket-types";
        }
        model.addAttribute("ticketType", dto);
        return "admin/tickettype/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("ticketType") TicketTypeDTO ticketType,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/tickettype/form";
        }

        ticketTypeService.saveTicketType(ticketType);
        redirectAttributes.addFlashAttribute("successMessage", "Lưu loại vé thành công!");
        return "redirect:/admin/ticket-types";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        ticketTypeService.deleteTicketType(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa (ẩn) loại vé thành công!");
        return "redirect:/admin/ticket-types";
    }
}
