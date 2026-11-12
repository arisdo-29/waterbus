package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Refund;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundAdminDTO(Integer id, String bookingCode, BigDecimal amount, String status,
                             String reason, String contactName, String contactPhone, String contactEmail,
                             String transactionRef, LocalDateTime refundedAt) {
    public static RefundAdminDTO fromEntity(Refund refund) {
        return new RefundAdminDTO(refund.getId(), refund.getBooking().getBookingCode(),
                refund.getAmount(), refund.getStatus(), refund.getReason(),
                refund.getBooking().getContactName(), refund.getBooking().getContactPhone(),
                refund.getBooking().getContactEmail(), refund.getTransactionRef(), refund.getRefundedAt());
    }
}
