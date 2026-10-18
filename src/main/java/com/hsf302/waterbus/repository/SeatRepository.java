package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface SeatRepository extends JpaRepository<Seat, Integer> {
}
