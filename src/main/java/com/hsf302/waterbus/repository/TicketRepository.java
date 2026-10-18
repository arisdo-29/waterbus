package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE4 - Đặt vé & Thanh toán
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
}
