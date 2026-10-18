package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.TicketTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. Mọi URL của controller này bắt đầu bằng /admin/ticket-types
// View đặt trong templates/tickettype/
@Controller
@RequestMapping("/admin/ticket-types")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping
    public String index() {
        // TODO BE3: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "tickettype/index";
    }
}
