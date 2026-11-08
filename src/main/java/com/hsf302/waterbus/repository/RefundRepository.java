package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Refund;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Integer> {
    boolean existsByBookingId(Integer bookingId);

    Optional<Refund> findByBookingId(Integer bookingId);

    List<Refund> findAllByOrderByCreatedAtDesc();

    List<Refund> findByStatusOrderByCreatedAtDesc(String status);

    List<Refund> findByStatusAndRefundedAtGreaterThanEqualAndRefundedAtLessThan(
            String status, LocalDateTime start, LocalDateTime end);
}
