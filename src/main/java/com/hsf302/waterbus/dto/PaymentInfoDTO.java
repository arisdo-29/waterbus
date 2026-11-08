package com.hsf302.waterbus.dto;

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

    public String getMethod() {
        return method;
    }

    public String getTransactionRef() {
        return transactionRef;
    }
}
