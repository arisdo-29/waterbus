package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.PaymentInfoDTO;
import com.hsf302.waterbus.entity.Payment;

public interface PaymentService {
    String getCannotPayReason(int bookingId);

    Payment confirmPaid(int bookingId, String method, String transactionRef);

    void confirmCashPayment(int bookingId, String loginRole);

    PaymentInfoDTO getPaymentOfBooking(int bookingId);
}
