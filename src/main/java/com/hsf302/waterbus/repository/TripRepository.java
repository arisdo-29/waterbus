package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface TripRepository extends JpaRepository<Trip, Integer> {
    @Query("""
            SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus
            WHERE t.status = 'SCHEDULED' AND t.route.isActive = true
              AND t.waterbus.status = 'ACTIVE'
              AND t.departureTime >= :start AND t.departureTime < :end
              AND EXISTS (
                  SELECT departure.id FROM RouteStation departure, RouteStation arrival
                  WHERE departure.route = t.route AND arrival.route = t.route
                    AND departure.station.id = :fromStationId
                    AND arrival.station.id = :toStationId
                    AND departure.stopOrder < arrival.stopOrder
              )
            ORDER BY t.departureTime, t.id
            """)
    List<Trip> searchTrips(@Param("fromStationId") int fromStationId,
                           @Param("toStationId") int toStationId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);
}
