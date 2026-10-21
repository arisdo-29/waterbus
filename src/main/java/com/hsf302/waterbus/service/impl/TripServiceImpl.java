package com.hsf302.waterbus.service.impl;

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
    public List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date) {
        throw new UnsupportedOperationException("TODO BE2");
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
