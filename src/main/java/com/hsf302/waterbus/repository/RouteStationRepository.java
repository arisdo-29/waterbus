package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.RouteStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

// Module: BE1 - Tuyến & Bến
public interface RouteStationRepository extends JpaRepository<RouteStation, Integer> {
    @EntityGraph(attributePaths = "station")
    List<RouteStation> findByRouteIdOrderByStopOrderAsc(int routeId);
}
