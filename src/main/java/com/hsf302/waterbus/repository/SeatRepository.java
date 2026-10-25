package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Module: BE2 - Tàu, Ghế & Chuyến
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    @Query("""
            SELECT COUNT(s) FROM Seat s
            WHERE s.waterbus.id = :waterbusId AND s.isActive = true
              AND NOT EXISTS (
                  SELECT t.id FROM Ticket t
                  WHERE t.trip.id = :tripId AND t.seat.id = s.id
                    AND t.status IN ('HOLD', 'VALID', 'USED')
              )
            """)
    long countAvailableSeats(@Param("tripId") int tripId, @Param("waterbusId") int waterbusId);

    @Query("""
            SELECT s FROM Seat s
            WHERE s.waterbus.id = :waterbusId AND s.isActive = true
              AND NOT EXISTS (
                  SELECT t.id FROM Ticket t
                  WHERE t.trip.id = :tripId AND t.seat.id = s.id
                    AND t.status IN ('HOLD', 'VALID', 'USED')
              )
            ORDER BY s.seatRow, s.seatCol, s.id
            """)
    List<Seat> findAvailableSeats(@Param("tripId") int tripId, @Param("waterbusId") int waterbusId);

    @Query("""
            SELECT s FROM Seat s
            WHERE s.waterbus.id = :waterbusId AND s.isActive = true
            ORDER BY s.seatRow, s.seatCol, s.id
            """)
    List<Seat> findActiveSeats(@Param("waterbusId") int waterbusId);

    @Query("""
            SELECT COUNT(s) FROM Seat s
            WHERE s.id = :seatId AND s.waterbus.id = :waterbusId AND s.isActive = true
              AND NOT EXISTS (
                  SELECT t.id FROM Ticket t
                  WHERE t.trip.id = :tripId AND t.seat.id = s.id
                    AND t.status IN ('HOLD', 'VALID', 'USED')
              )
            """)
    long countAvailableSeat(@Param("tripId") int tripId, @Param("waterbusId") int waterbusId,
                            @Param("seatId") int seatId);
}
