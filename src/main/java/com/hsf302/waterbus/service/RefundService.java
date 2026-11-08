package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.RefundAdminDTO;
import com.hsf302.waterbus.entity.Refund;
import java.time.LocalDateTime;
import java.util.List;

public interface RefundService {
    void createForCancelledTrip(int tripId, String reason);

    List<RefundAdminDTO> getRefunds(String status);

    Refund getRefundOfBooking(int bookingId);

    void markRefunded(int id, String transactionRef, LocalDateTime refundedAt, String note);
}
