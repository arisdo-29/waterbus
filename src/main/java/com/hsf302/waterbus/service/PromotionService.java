package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.PromotionDTO;
import com.hsf302.waterbus.entity.Promotion;
import java.math.BigDecimal;
import java.util.List;

import java.util.Optional;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface PromotionService {
    /** Danh sách khuyến mãi (trang admin). */
    List<Promotion> getPromotions();

    /** HỢP ĐỒNG cho BE4: số tiền được giảm. Mã sai/hết hạn/hết lượt/chưa đủ min_order -> 0. */
    BigDecimal calculateDiscount(String promoCode, BigDecimal subtotal);

    /** Lấy khuyến mãi theo mã (nếu hợp lệ). */
    Optional<Promotion> getPromotionByCode(String promoCode);

    /** Tăng số lượt đã dùng khi đơn hàng thanh toán thành công. */
    void increaseUsedCount(String promoCode);

    /** Lấy danh sách DTO cho trang quản trị. */
    List<PromotionDTO> getAllPromotions();

    /** Lấy chi tiết khuyến mãi theo ID. */
    PromotionDTO getPromotionById(int id);

    /** Lưu (thêm mới hoặc cập nhật) khuyến mãi. */
    void savePromotion(PromotionDTO dto);

    /** Xoá mềm (ẩn) khuyến mãi. */
    void deletePromotion(int id);
}
