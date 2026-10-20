package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Module: BE1 - Tuyến & Bến
public interface StationRepository extends JpaRepository<Station, Integer> {
    List<Station> findByIsActiveTrueOrderByIdAsc();

    Optional<Station> findFirstByCodeIgnoreCase(String code);
}
