package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface TripRepository extends JpaRepository<Trip, Integer> {
    long countByWaterbusIdAndStatusAndDepartureTimeAfter(Integer waterbusId, String status,
                                                          LocalDateTime departureTime);
    @Query("SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus ORDER BY t.departureTime DESC, t.id DESC")
    List<Trip> findAllForAdmin();

    boolean existsByTripCode(String tripCode);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.waterbus.id = :waterbusId AND t.status = 'SCHEDULED'
              AND t.departureTime < :arrivalTime AND t.arrivalTime > :departureTime
            """)
    long countScheduledOverlaps(@Param("waterbusId") int waterbusId,
                                @Param("departureTime") LocalDateTime departureTime,
                                @Param("arrivalTime") LocalDateTime arrivalTime);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus "
            + "WHERE t.departureTime >= :start AND t.departureTime < :end ORDER BY t.departureTime, t.id")
    List<Trip> findDepartingBetween(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("""
            SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus
            WHERE t.status = 'SCHEDULED' AND t.route.isActive = true
              AND t.waterbus.status = 'ACTIVE' AND t.route.id = :routeId
              AND t.departureTime >= :start AND t.departureTime < :end
            ORDER BY t.departureTime, t.id
            """)
    List<Trip> searchTripsForRoute(@Param("routeId") int routeId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);
}
