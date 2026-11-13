package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    List<Booking> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    List<Booking> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);

    List<Booking> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}
