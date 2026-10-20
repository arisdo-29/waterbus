package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface TicketTypeRepository extends JpaRepository<TicketType, Integer> {
    List<TicketType> findByIsActiveTrue();
    Optional<TicketType> findByCode(String code);
}
