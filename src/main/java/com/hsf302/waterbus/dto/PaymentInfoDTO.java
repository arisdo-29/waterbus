package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Payment;
import java.io.Serializable;

public class PaymentInfoDTO implements Serializable {
    private final String method;
    private final String transactionRef;

    public PaymentInfoDTO(String method, String transactionRef) {
        this.method = method;
        this.transactionRef = transactionRef;
    }

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
