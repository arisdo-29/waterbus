package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Payment;
import java.io.Serializable;

// Module: BE4 - Đặt vé & Thanh toán. Thông tin thanh toán đưa ra trang xem vé (model "payment").
// Chỉ lấy 2 thứ khách cần xem, không đưa cả entity Payment ra view.
public class PaymentInfoDTO implements Serializable {
    private final String method;         // MOCK | VNPAY | MOMO
    private final String transactionRef; // mã giao dịch, có thể null

    public PaymentInfoDTO(String method, String transactionRef) {
        this.method = method;
        this.transactionRef = transactionRef;
    }

    // Entity Payment -> DTO. Đặt ở đây (không đặt trong ServiceImpl) để việc "lấy field nào ra view"
    // nằm cùng chỗ với các field của DTO; ServiceImpl chỉ cần gọi PaymentInfoDTO::fromEntity.
    public static PaymentInfoDTO fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentInfoDTO(payment.getMethod(), payment.getTransactionRef());
    }

    public String getMethod() {
        return method;
    }

    public String getTransactionRef() {
        return transactionRef;
    }
}
