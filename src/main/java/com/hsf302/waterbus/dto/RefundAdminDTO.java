package com.hsf302.waterbus.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundAdminDTO(Integer id, String bookingCode, BigDecimal amount, String status,
                             String reason, String transactionRef, LocalDateTime refundedAt) {
}
