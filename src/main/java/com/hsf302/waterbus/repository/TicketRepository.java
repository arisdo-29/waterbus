package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findByBookingIdOrderByIdAsc(Integer bookingId);

    List<Ticket> findByBooking_BookingCodeOrderByIdAsc(String bookingCode);

    List<Ticket> findByTripIdOrderByIdAsc(Integer tripId);

    Optional<Ticket> findByTicketCode(String ticketCode);
}
