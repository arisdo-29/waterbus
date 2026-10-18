package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Promotion;
import java.math.BigDecimal;
import java.util.List;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface PromotionService {
    /** Danh sách khuyến mãi (trang admin). */
    List<Promotion> getPromotions();

    /** HỢP ĐỒNG cho BE4: số tiền được giảm. Mã sai/hết hạn/hết lượt/chưa đủ min_order -> 0. */
    BigDecimal calculateDiscount(String promoCode, BigDecimal subtotal);
}
