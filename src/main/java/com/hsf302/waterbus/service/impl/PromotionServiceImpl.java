package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Promotion;
import com.hsf302.waterbus.service.PromotionService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. TODO: thay các dòng throw bằng code thật.
@Service
public class PromotionServiceImpl implements PromotionService {

    @Override
    public List<Promotion> getPromotions() {
        throw new UnsupportedOperationException("TODO BE3");
    }

    @Override
    public BigDecimal calculateDiscount(String promoCode, BigDecimal subtotal) {
        throw new UnsupportedOperationException("TODO BE3");
    }
}
