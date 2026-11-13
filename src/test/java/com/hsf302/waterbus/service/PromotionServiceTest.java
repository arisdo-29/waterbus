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
import static org.mockito.ArgumentMatchers.any;
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
        promo.setDiscountValue(BigDecimal.valueOf(10));
        promo.setMaxDiscount(BigDecimal.valueOf(20000));
        promo.setMinOrderAmount(BigDecimal.valueOf(50000));
        promo.setStartAt(LocalDateTime.now().minusDays(1));
        promo.setEndAt(LocalDateTime.now().plusDays(1));
        promo.setUsageLimit(100);
        promo.setUsedCount(5);
        promo.setIsActive(true);

        when(promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue("GIAM10")).thenReturn(Optional.of(promo));

        BigDecimal discount = promotionService.calculateDiscount("GIAM10", BigDecimal.valueOf(100000));
        assertEquals(BigDecimal.valueOf(10000), discount);

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

        BigDecimal discount = promotionService.calculateDiscount("FIXED15K", BigDecimal.valueOf(50000));
        assertEquals(BigDecimal.valueOf(15000), discount);

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
        promo.setEndAt(LocalDateTime.now().minusDays(1));
        promo.setIsActive(true);

        when(promotionRepository.findByCodeIgnoreCaseAndIsActiveTrue("HETHAN")).thenReturn(Optional.of(promo));

        BigDecimal discount = promotionService.calculateDiscount("HETHAN", BigDecimal.valueOf(50000));
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testIsCodeDuplicate() {
        Promotion existing = new Promotion();
        existing.setId(10);
        existing.setCode("WELCOME10");

        when(promotionRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(Optional.of(existing));
        when(promotionRepository.findByCodeIgnoreCase("NEWCODE")).thenReturn(Optional.empty());

        assertEquals(true, promotionService.isCodeDuplicate("WELCOME10", null));
        assertEquals(false, promotionService.isCodeDuplicate("NEWCODE", null));
        assertEquals(false, promotionService.isCodeDuplicate("WELCOME10", 10));
        assertEquals(true, promotionService.isCodeDuplicate("WELCOME10", 99));
    }

    @Test
    void testIncreaseUsedCount_Success() {
        Promotion promo = new Promotion();
        promo.setCode("PROMO10");
        promo.setUsageLimit(10);
        promo.setUsedCount(5);

        when(promotionRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));

        promotionService.increaseUsedCount("PROMO10");

        assertEquals(6, promo.getUsedCount());
        Mockito.verify(promotionRepository).save(promo);
    }

    @Test
    void testIncreaseUsedCount_ExceedsLimit_ThrowsException() {
        Promotion promo = new Promotion();
        promo.setCode("PROMO_FULL");
        promo.setUsageLimit(5);
        promo.setUsedCount(5);

        when(promotionRepository.findByCodeIgnoreCase("PROMO_FULL")).thenReturn(Optional.of(promo));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> promotionService.increaseUsedCount("PROMO_FULL"),
                "Mã khuyến mãi đã hết lượt sử dụng."
        );
    }

    @Test
    void testDecreaseUsedCount_Success() {
        Promotion promo = new Promotion();
        promo.setCode("PROMO10");
        promo.setUsageLimit(10);
        promo.setUsedCount(5);

        when(promotionRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));

        promotionService.decreaseUsedCount("PROMO10");

        assertEquals(4, promo.getUsedCount());
        Mockito.verify(promotionRepository).save(promo);
    }

    @Test
    void testDecreaseUsedCount_ZeroCount_DoesNotGoBelowZero() {
        Promotion promo = new Promotion();
        promo.setCode("PROMO0");
        promo.setUsageLimit(10);
        promo.setUsedCount(0);

        when(promotionRepository.findByCodeIgnoreCase("PROMO0")).thenReturn(Optional.of(promo));

        promotionService.decreaseUsedCount("PROMO0");

        assertEquals(0, promo.getUsedCount());
        Mockito.verify(promotionRepository, Mockito.never()).save(promo);
    }

    @Test
    void testDecreaseUsedCount_BlankOrNotFound_DoesNothing() {
        promotionService.decreaseUsedCount(null);
        promotionService.decreaseUsedCount("   ");

        when(promotionRepository.findByCodeIgnoreCase("NONEXISTENT")).thenReturn(Optional.empty());
        promotionService.decreaseUsedCount("NONEXISTENT");

        Mockito.verify(promotionRepository, Mockito.never()).save(any());
    }
}
