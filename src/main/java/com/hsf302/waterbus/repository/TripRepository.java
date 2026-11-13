package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

public interface TripRepository extends JpaRepository<Trip, Integer> {
    long countByWaterbusIdAndStatusAndDepartureTimeAfter(Integer waterbusId, String status,
                                                          LocalDateTime departureTime);
    long countByRouteIdAndStatusAndDepartureTimeAfter(Integer routeId, String status,
                                                      LocalDateTime departureTime);
    @Query("SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus ORDER BY t.departureTime DESC, t.id DESC")
    List<Trip> findAllForAdmin();

    @Query("""
            SELECT t FROM Trip t JOIN FETCH t.route JOIN FETCH t.waterbus
            WHERE t.status = 'SCHEDULED' AND t.waterbus.id = :waterbusId
              AND t.departureTime > :now
              AND t.departureTime >= :departureFrom AND t.departureTime <= :departureTo
            ORDER BY t.departureTime, t.id
            """)
    List<Trip> findCancellableForPreview(@Param("waterbusId") int waterbusId,
                                         @Param("departureFrom") LocalDateTime departureFrom,
                                         @Param("departureTo") LocalDateTime departureTo,
                                         @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") int id);

    boolean existsByTripCode(String tripCode);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.waterbus.id = :waterbusId AND t.status IN ('SCHEDULED', 'DEPARTED')
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
            WHERE t.status IN ('SCHEDULED', 'DEPARTED') AND t.route.isActive = true
              AND t.waterbus.status = 'ACTIVE' AND t.route.id = :routeId
              AND t.departureTime >= :start AND t.departureTime < :end
            ORDER BY t.departureTime, t.id
            """)
    List<Trip> searchTripsForRoute(@Param("routeId") int routeId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Modifying
    @Query("UPDATE Trip t SET t.status = 'COMPLETED' "
            + "WHERE t.status IN ('SCHEDULED', 'DEPARTED') AND t.arrivalTime <= :now")
    int completeArrivedTrips(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Trip t SET t.status = 'DEPARTED' "
            + "WHERE t.status = 'SCHEDULED' AND t.departureTime <= :now AND t.arrivalTime > :now")
    int departStartedTrips(@Param("now") LocalDateTime now);
}
