package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.TripSearchDTO;
import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.SeatRepository;
import com.hsf302.waterbus.repository.TripRepository;
import com.hsf302.waterbus.service.TripService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Module: BE2 - Tàu, Ghế & Chuyến.
@Service
public class TripServiceImpl implements TripService {
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;

    public TripServiceImpl(TripRepository tripRepository, SeatRepository seatRepository) {
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date) {
        if (date == null || fromStationId <= 0 || toStationId <= 0 || fromStationId == toStationId) {
            return List.of();
        }
        return tripRepository.searchTrips(fromStationId, toStationId,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSearchDTO> searchTripResults(int fromStationId, int toStationId, LocalDate date) {
        return searchTrips(fromStationId, toStationId, date).stream()
                .map(trip -> new TripSearchDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                        trip.getDepartureTime(), trip.getArrivalTime(),
                        seatRepository.countAvailableSeats(trip.getId(), trip.getWaterbus().getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(int tripId) {
        return tripRepository.findById(tripId)
                .map(trip -> seatRepository.findAvailableSeats(tripId, trip.getWaterbus().getId()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSeatAvailable(int tripId, int seatId) {
        return tripRepository.findById(tripId)
                .map(trip -> seatRepository.countAvailableSeat(tripId, trip.getWaterbus().getId(), seatId) > 0)
                .orElse(false);
    }
}
