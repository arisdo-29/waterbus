package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Module: BE4 - Đặt vé & Thanh toán
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    // "BookingId" = field booking -> id của nó: WHERE booking_id = ? ORDER BY id ASC
    List<Ticket> findByBookingIdOrderByIdAsc(Integer bookingId);
}
