package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Promotion;
import com.hsf302.waterbus.repository.PromotionRepository;
import com.hsf302.waterbus.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    private PromotionRepository promotionRepository;
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionRepository = Mockito.mock(PromotionRepository.class);
        promotionService = new PromotionServiceImpl(promotionRepository);
    }

    @Test
    void testCalculateDiscount_InvalidOrEmptyCode() {
        BigDecimal discount = promotionService.calculateDiscount("", BigDecimal.valueOf(100000));
        assertEquals(BigDecimal.ZERO, discount);

        discount = promotionService.calculateDiscount(null, BigDecimal.valueOf(100000));
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testCalculateDiscount_PercentPromotion_WithMaxDiscount() {
        Promotion promo = new Promotion();
        promo.setCode("GIAM10");
        promo.setDiscountType("PERCENT");
        promo.setDiscountValue(BigDecimal.valueOf(10)); // 10%
        promo.setMaxDiscount(BigDecimal.valueOf(20000)); // trần 20k
        promo.setMinOrderAmount(BigDecimal.valueOf(50000));
        promo.setStartAt(LocalDateTime.now().minusDays(1));
        promo.setEndAt(LocalDateTime.now().plusDays(1));
        promo.setUsageLimit(100);
        promo.setUsedCount(5);
        promo.setIsActive(true);

        when(promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue("GIAM10")).thenReturn(Optional.of(promo));

        // Subtotal 100k -> 10% = 10k (nhỏ hơn max 20k) -> giảm 10k
        BigDecimal discount = promotionService.calculateDiscount("GIAM10", BigDecimal.valueOf(100000));
        assertEquals(BigDecimal.valueOf(10000), discount);

        // Subtotal 300k -> 10% = 30k (vượt trần 20k) -> giảm đúng 20k
        discount = promotionService.calculateDiscount("GIAM10", BigDecimal.valueOf(300000));
        assertEquals(BigDecimal.valueOf(20000), discount);
    }

    @Test
    void testCalculateDiscount_FixedPromotion() {
        Promotion promo = new Promotion();
        promo.setCode("FIXED15K");
        promo.setDiscountType("FIXED");
        promo.setDiscountValue(BigDecimal.valueOf(15000));
        promo.setMinOrderAmount(BigDecimal.valueOf(30000));
        promo.setStartAt(LocalDateTime.now().minusDays(1));
        promo.setEndAt(LocalDateTime.now().plusDays(1));
        promo.setIsActive(true);

        when(promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue("FIXED15K")).thenReturn(Optional.of(promo));

        // Đơn 50k -> giảm 15k
        BigDecimal discount = promotionService.calculateDiscount("FIXED15K", BigDecimal.valueOf(50000));
        assertEquals(BigDecimal.valueOf(15000), discount);

        // Đơn 20k (chưa đủ min 30k) -> giảm 0
        discount = promotionService.calculateDiscount("FIXED15K", BigDecimal.valueOf(20000));
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testCalculateDiscount_Expired() {
        Promotion promo = new Promotion();
        promo.setCode("HETHAN");
        promo.setDiscountType("FIXED");
        promo.setDiscountValue(BigDecimal.valueOf(10000));
        promo.setStartAt(LocalDateTime.now().minusDays(5));
        promo.setEndAt(LocalDateTime.now().minusDays(1)); // Đã hết hạn
        promo.setIsActive(true);

        when(promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue("HETHAN")).thenReturn(Optional.of(promo));

        BigDecimal discount = promotionService.calculateDiscount("HETHAN", BigDecimal.valueOf(50000));
        assertEquals(BigDecimal.ZERO, discount);
    }
}
