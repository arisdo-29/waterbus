package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE1 - Tuyến & Bến
public interface StationRepository extends JpaRepository<Station, Integer> {
}
