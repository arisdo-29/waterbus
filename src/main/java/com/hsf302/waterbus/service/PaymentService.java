package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.PaymentInfoDTO;
import com.hsf302.waterbus.entity.Payment;

// Module: BE4 - Đặt vé & Thanh toán
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
// Muốn thêm cổng thanh toán thật (VNPay, MoMo...): tạo class mới implements PaymentService, controller chỉ gọi interface nên không phải sửa.
public interface PaymentService {
    /** Thanh toán giả lập: payment SUCCESS, booking PAID, vé HOLD -> VALID. */
    Payment payMock(int bookingId);

    /** Thông tin thanh toán của 1 đơn cho trang xem vé (phương thức, mã giao dịch). Đơn chưa có payment -> null. */
    PaymentInfoDTO getPaymentOfBooking(int bookingId);
}
