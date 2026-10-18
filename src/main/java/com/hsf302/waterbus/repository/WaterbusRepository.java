package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Waterbus;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface WaterbusRepository extends JpaRepository<Waterbus, Integer> {
}
