package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Payment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE4 - Đặt vé & Thanh toán
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            String status, LocalDateTime start, LocalDateTime end);

    List<Payment> findByBookingIdAndStatus(Integer bookingId, String status);
    // WHERE booking_id = ? -> payment của 1 đơn. DB cho mỗi đơn tối đa 1 payment, nên dùng Optional (có hoặc không)
    Optional<Payment> findByBookingId(Integer bookingId);
}
