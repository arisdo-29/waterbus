package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.PromotionDTO;
import com.hsf302.waterbus.entity.Promotion;
import com.hsf302.waterbus.repository.PromotionRepository;
import com.hsf302.waterbus.service.PromotionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi.
@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public List<Promotion> getPromotions() {
        return promotionRepository.findAll();
    }

    @Override
    public BigDecimal calculateDiscount(String promoCode, BigDecimal subtotal) {
        if (promoCode == null || promoCode.trim().isEmpty() || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        Optional<Promotion> promoOpt = promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(promoCode.trim());
        if (promoOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Promotion promo = promoOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 1. Kiểm tra thời hạn hiệu lực
        if (promo.getStartAt() != null && now.isBefore(promo.getStartAt())) {
            return BigDecimal.ZERO;
        }
        if (promo.getEndAt() != null && now.isAfter(promo.getEndAt())) {
            return BigDecimal.ZERO;
        }

        // 2. Kiểm tra giới hạn lượt sử dụng
        if (promo.getUsageLimit() != null && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return BigDecimal.ZERO;
        }

        // 3. Kiểm tra đơn hàng tối thiểu
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        // 4. Tính toán số tiền giảm
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENT".equalsIgnoreCase(promo.getDiscountType())) {
            discount = subtotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
                discount = promo.getMaxDiscount();
            }
        } else if ("FIXED".equalsIgnoreCase(promo.getDiscountType())) {
            discount = promo.getDiscountValue();
        }

        // 5. Ràng buộc: discount không vượt quá subtotal và không âm
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        return discount;
    }

    @Override
    public Optional<Promotion> getPromotionByCode(String promoCode) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue(promoCode.trim());
    }

    @Override
    @Transactional
    public void increaseUsedCount(String promoCode) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return;
        }
        promotionRepository.findByCodeIgnoreCase(promoCode.trim()).ifPresent(promo -> {
            int current = promo.getUsedCount() != null ? promo.getUsedCount() : 0;
            promo.setUsedCount(current + 1);
            promotionRepository.save(promo);
        });
    }

    @Override
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public PromotionDTO getPromotionById(int id) {
        return promotionRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public void savePromotion(PromotionDTO dto) {
        if (dto == null) {
            return;
        }

        Promotion promo;
        if (dto.getId() != null) {
            promo = promotionRepository.findById(dto.getId())
                    .orElse(new Promotion());
        } else {
            promo = new Promotion();
            promo.setUsedCount(0);
        }

        if (dto.getCode() != null) {
            promo.setCode(dto.getCode().trim().toUpperCase());
        }
        promo.setName(dto.getName());
        promo.setDiscountType(dto.getDiscountType());
        promo.setDiscountValue(dto.getDiscountValue());
        if ("FIXED".equalsIgnoreCase(dto.getDiscountType())) {
            promo.setMaxDiscount(null);
        } else {
            promo.setMaxDiscount(dto.getMaxDiscount());
        }
        promo.setMinOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : BigDecimal.ZERO);
        promo.setStartAt(dto.getStartAt());
        promo.setEndAt(dto.getEndAt());
        promo.setUsageLimit(dto.getUsageLimit());
        promo.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        promotionRepository.save(promo);
    }

    @Override
    public boolean isCodeDuplicate(String code, Integer excludeId) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        Optional<Promotion> existing = promotionRepository.findByCodeIgnoreCase(code.trim());
        if (existing.isEmpty()) {
            return false;
        }
        return excludeId == null || !existing.get().getId().equals(excludeId);
    }

    @Override
    @Transactional
    public void deletePromotion(int id) {
        promotionRepository.findById(id).ifPresent(promo -> {
            promo.setIsActive(false);
            promotionRepository.save(promo);
        });
    }

    private PromotionDTO toDTO(Promotion p) {
        return new PromotionDTO(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getDiscountType(),
                p.getDiscountValue(),
                p.getMaxDiscount(),
                p.getMinOrderAmount(),
                p.getStartAt(),
                p.getEndAt(),
                p.getUsageLimit(),
                p.getUsedCount(),
                p.getIsActive()
        );
    }
}
