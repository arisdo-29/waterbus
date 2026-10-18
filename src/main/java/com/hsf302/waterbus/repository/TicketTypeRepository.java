package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface TicketTypeRepository extends JpaRepository<TicketType, Integer> {
}
