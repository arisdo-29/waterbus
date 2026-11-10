package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface TripRepository extends JpaRepository<Trip, Integer> {
    @Query("SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus ORDER BY t.departureTime DESC, t.id DESC")
    List<Trip> findAllForAdmin();

    boolean existsByTripCode(String tripCode);

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
