package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Module: BE4 - Đặt vé & Thanh toán
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    // Spring tự sinh câu SQL từ tên method: WHERE booking_code = ?
    Optional<Booking> findByBookingCode(String bookingCode);
}
