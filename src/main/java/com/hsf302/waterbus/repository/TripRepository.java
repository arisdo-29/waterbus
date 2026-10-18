package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface TripRepository extends JpaRepository<Trip, Integer> {
}
