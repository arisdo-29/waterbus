package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.TicketScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketScanLogRepository extends JpaRepository<TicketScanLog, Integer> {

    List<TicketScanLog> findByTripIdOrderByCreatedAtDesc(Integer tripId);

    List<TicketScanLog> findByStaffIdOrderByCreatedAtDesc(Integer staffId);

    List<TicketScanLog> findByTicketCodeOrderByCreatedAtDesc(String ticketCode);

    List<TicketScanLog> findByBookingCodeOrderByCreatedAtDesc(String bookingCode);
}

