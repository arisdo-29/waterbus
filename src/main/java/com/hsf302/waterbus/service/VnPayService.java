package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.VnPayResult;
import java.util.Map;

// Module: BE4 - Đặt vé & Thanh toán. Thanh toán qua cổng VNPay Sandbox.
public interface VnPayService {
    /**
     * Tạo đường link sang trang thanh toán VNPay cho 1 đơn (KHÔNG ghi gì vào DB).
     * Đơn không thanh toán được hoặc tổng tiền <= 0 -> IllegalArgumentException.
     */
    String createPaymentUrl(String bookingCode, String clientIp);

    /** Xử lý các tham số VNPay gửi kèm khi trả khách về: kiểm tra chữ ký, số tiền, kết quả rồi ghi nhận thanh toán. */
    VnPayResult handleReturn(Map<String, String> params);
}
