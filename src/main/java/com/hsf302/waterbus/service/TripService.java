package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingTripDTO;
import com.hsf302.waterbus.dto.AdminTripDTO;
import com.hsf302.waterbus.dto.AdminTripForm;
import com.hsf302.waterbus.dto.SeatDTO;
import com.hsf302.waterbus.dto.TripCancellationPreviewDTO;
import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.dto.TripManifestDTO;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TripService {
    int ONLINE_SALES_CUTOFF_MINUTES = 10;

    List<AdminTripDTO> getTripsForAdmin();

    List<TripCancellationPreviewDTO> getCancellableTrips(int waterbusId, LocalDateTime departureFrom,
                                                          LocalDateTime departureTo);

    List<AdminTripDTO> getTodayTrips();

    TripManifestDTO getManifest(int tripId);

    long countUpcomingTripsOfRoute(int routeId);

    void createAdminTrip(AdminTripForm form);

    void cancelTrip(int tripId, String reason);

    void cancelTrips(List<Integer> tripIds, String reason);

    void cancelTrips(List<Integer> tripIds, String reason, int waterbusId,
                    LocalDateTime departureFrom, LocalDateTime departureTo);

    List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date);

    List<TripSearchDTO> searchTripResults(int fromStationId, int toStationId, LocalDate date);

    BookingTripDTO getBookableTrip(int tripId);

    BookingTripDTO getBookableTrip(int tripId, int fromStationId, int toStationId);

    Trip getTripById(int tripId);

    List<SeatDTO> getSeatMap(int tripId);

    List<Seat> getAvailableSeats(int tripId);

    boolean isSeatAvailable(int tripId, int seatId);
}
