package com.hsf302.waterbus.controller;

import com.hsf302.waterbus.dto.PromotionDTO;
import com.hsf302.waterbus.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. Mọi URL của controller này bắt đầu bằng /admin/promotions
// View đặt trong templates/admin/promotion/ (giao diện: Tân)
@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions());
        return "admin/promotion/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        if (!model.containsAttribute("promotion")) {
            model.addAttribute("promotion", new PromotionDTO());
        }
        return "admin/promotion/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        PromotionDTO dto = promotionService.getPromotionById(id);
        if (dto == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khuyến mãi.");
            return "redirect:/admin/promotions";
        }
        model.addAttribute("promotion", dto);
        return "admin/promotion/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("promotion") PromotionDTO promotion,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (promotion.getCode() != null && !promotion.getCode().trim().isEmpty()) {
            String normalizedCode = promotion.getCode().trim().toUpperCase();
            promotion.setCode(normalizedCode);
            if (promotionService.isCodeDuplicate(normalizedCode, promotion.getId())) {
                bindingResult.rejectValue("code", "error.duplicate", "Mã khuyến mãi đã tồn tại trong hệ thống");
            }
        }

        if ("PERCENT".equalsIgnoreCase(promotion.getDiscountType())) {
            if (promotion.getDiscountValue() != null && promotion.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                bindingResult.rejectValue("discountValue", "error.percent", "Giá trị giảm theo phần trăm không được vượt quá 100%");
            }
        }

        if (promotion.getStartAt() != null && promotion.getEndAt() != null &&
                !promotion.getEndAt().isAfter(promotion.getStartAt())) {
            bindingResult.rejectValue("endAt", "error.endAt", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (bindingResult.hasErrors()) {
            return "admin/promotion/form";
        }

        try {
            promotionService.savePromotion(promotion);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu khuyến mãi thành công!");
            return "redirect:/admin/promotions";
        } catch (Exception e) {
            bindingResult.reject("error.save", "Không thể lưu khuyến mãi. Vui lòng kiểm tra lại thông tin.");
            return "admin/promotion/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        promotionService.deletePromotion(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa (ẩn) khuyến mãi thành công!");
        return "redirect:/admin/promotions";
    }
}
