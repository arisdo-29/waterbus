package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.VnPayResult;
import java.util.Map;

// Module: BE4 - Đặt vé & Thanh toán. Thanh toán qua cổng VNPay Sandbox.
public interface VnPayService {
    /**
     * Tạo đường link sang trang thanh toán VNPay cho 1 đơn (KHÔNG ghi gì vào DB).
     * Đơn không thanh toán được hoặc tổng tiền <= 0 -> IllegalArgumentException.
     * bankCode: "VNPAYQR" (mở thẳng màn hình QR) hoặc "VNBANK" (mở thẳng trang thẻ ATM nội địa);
     * null / mã khác -> không gửi, VNPay hiện trang cho khách tự chọn phương thức.
     */
    String createPaymentUrl(String bookingCode, String clientIp, String bankCode);

    /** Bản cũ 2 tham số: giữ lại để code và test cũ không phải sửa, tương đương không chọn sẵn phương thức. */
    default String createPaymentUrl(String bookingCode, String clientIp) {
        return createPaymentUrl(bookingCode, clientIp, null);
    }

    /** Xử lý các tham số VNPay gửi kèm khi trả khách về: kiểm tra chữ ký, số tiền, kết quả rồi ghi nhận thanh toán. */
    VnPayResult handleReturn(Map<String, String> params);
}
