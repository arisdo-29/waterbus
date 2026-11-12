package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Refund;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundViewDTO(String status, BigDecimal amount, LocalDateTime refundedAt,
                            String transactionRef) {
    public static RefundViewDTO fromEntity(Refund refund) {
        return new RefundViewDTO(refund.getStatus(), refund.getAmount(), refund.getRefundedAt(),
                refund.getTransactionRef());
    }
}
