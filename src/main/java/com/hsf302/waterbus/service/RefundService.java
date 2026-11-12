package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RefundAdminDTO;
import com.hsf302.waterbus.dto.RefundViewDTO;
import com.hsf302.waterbus.entity.Refund;
import java.util.List;

public interface RefundService {
    void createForCancelledTrip(int tripId, String reason);

    /** HỢP ĐỒNG cho BE4: gọi sau khi đã lưu payment SUCCESS cho đơn không còn thanh toán được. */
    void createForInvalidPayment(int bookingId, String reason);

    List<RefundAdminDTO> getRefunds(String status);

    Refund getRefundOfBooking(int bookingId);

    RefundViewDTO getRefundViewOfBooking(int bookingId);

    void markRefunded(int id, AccountDTO admin);
}
