package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

// Module: BE1 - Tuyến & Bến
public interface RouteRepository extends JpaRepository<Route, Integer> {
    List<Route> findByIsActiveTrueOrderByIdAsc();

    List<Route> findAllByOrderByIdAsc();

    Optional<Route> findFirstByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Route r where r.id = :id")
    Optional<Route> findForUpdate(int id);
}
