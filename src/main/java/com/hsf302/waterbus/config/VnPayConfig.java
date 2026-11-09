package com.hsf302.waterbus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Module: BE4 - Đặt vé & Thanh toán. Cấu hình cổng VNPay Sandbox.
// Khoá thật (tmn-code, hash-secret) KHÔNG ghi vào code hay application.properties:
// mỗi máy tự điền vào application-local.properties (file bị .gitignore), VD:
//   vnpay.tmn-code=ABCD1234
//   vnpay.hash-secret=XXXXXXXXXXXXXXXX
// Chữ sau dấu ":" trong ${...} là giá trị mặc định khi máy không khai báo khoá đó.
@Component
public class VnPayConfig {

    private final String tmnCode;    // mã website (Terminal ID) VNPay cấp
    private final String hashSecret; // chuỗi bí mật để ký dữ liệu
    private final String payUrl;     // trang thanh toán của VNPay
    private final String returnUrl;  // VNPay trả khách về địa chỉ này sau khi thanh toán

    // Constructor injection: Spring đọc từng khoá rồi truyền vào đây
    public VnPayConfig(@Value("${vnpay.tmn-code:}") String tmnCode,
                       @Value("${vnpay.hash-secret:}") String hashSecret,
                       @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
                       @Value("${vnpay.return-url:http://localhost:8080/payment/vnpay-return}") String returnUrl) {
        // trim(): lỡ gõ thừa khoảng trắng trong file cấu hình thì vẫn đúng
        this.tmnCode = tmnCode.trim();
        this.hashSecret = hashSecret.trim();
        this.payUrl = payUrl.trim();
        this.returnUrl = returnUrl.trim();
    }

    // Có đủ 2 khoá bí mật thì mới dùng được VNPay; thiếu thì trang thanh toán báo dùng Thanh toán mô phỏng
    public boolean isConfigured() {
        return !tmnCode.isEmpty() && !hashSecret.isEmpty();
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
}
