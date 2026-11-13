package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.PaymentInfoDTO;
import com.hsf302.waterbus.entity.Payment;

// Module: BE4 - Đặt vé & Thanh toán
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
// Muốn thêm cổng thanh toán thật (VNPay, MoMo...): tạo class mới implements PaymentService, controller chỉ gọi interface nên không phải sửa.
public interface PaymentService {
    /**
     * Đơn có thanh toán được không? Được -> null. Không được -> câu lỗi để hiện cho khách
     * (không tìm thấy đơn / đơn không còn PENDING / hết hạn giữ chỗ / chuyến không còn SCHEDULED hoặc DEPARTED / mã KM hết lượt).
     */
    String getCannotPayReason(int bookingId);

    /**
     * Ghi nhận đơn đã được trả tiền (VNPay gọi khi khách trả tiền thành công): payment SUCCESS, booking PAID,
     * vé HOLD -> VALID, tăng lượt dùng mã KM. Đơn không thanh toán được -> IllegalArgumentException.
     */
    Payment confirmPaid(int bookingId, String method, String transactionRef);

    /** Thông tin thanh toán của 1 đơn cho trang xem vé (phương thức, mã giao dịch). Đơn chưa có payment -> null. */
    PaymentInfoDTO getPaymentOfBooking(int bookingId);
}
