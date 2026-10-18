package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. Mọi URL của controller này bắt đầu bằng /admin/promotions
// View đặt trong templates/promotion/
@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String index() {
        // TODO BE3: lấy dữ liệu từ service, model.addAttribute(...), trả về view
        return "promotion/index";
    }
}
