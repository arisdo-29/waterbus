package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Payment;
import java.util.List;

// Module: BE4 - Đặt vé & Thanh toán
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface PaymentService {
    /** Thanh toán giả lập: payment SUCCESS, booking PAID, vé HOLD -> VALID. */
    Payment payMock(int bookingId);
}
