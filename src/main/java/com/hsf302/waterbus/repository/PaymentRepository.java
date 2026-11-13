package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Payment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByStatusAndPaidAtGreaterThanEqualAndPaidAtLessThan(
            String status, LocalDateTime start, LocalDateTime end);

    List<Payment> findByBookingIdAndStatus(Integer bookingId, String status);
    Optional<Payment> findByBookingId(Integer bookingId);
}
