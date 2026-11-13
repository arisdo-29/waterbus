package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.PromotionDTO;
import com.hsf302.waterbus.entity.Promotion;
import java.math.BigDecimal;
import java.util.List;

import java.util.Optional;

public interface PromotionService {
    List<Promotion> getPromotions();

    BigDecimal calculateDiscount(String promoCode, BigDecimal subtotal);

    Optional<Promotion> getPromotionByCode(String promoCode);

    void increaseUsedCount(String promoCode);

    void decreaseUsedCount(String promoCode);

    List<PromotionDTO> getAllPromotions();

    PromotionDTO getPromotionById(int id);

    void savePromotion(PromotionDTO dto);

    void deletePromotion(int id);

    boolean isCodeDuplicate(String code, Integer excludeId);
}
