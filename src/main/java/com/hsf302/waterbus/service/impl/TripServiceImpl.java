package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Seat;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.service.TripService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE2 - Tàu, Ghế & Chuyến. TODO: thay các dòng throw bằng code thật.
@Service
public class TripServiceImpl implements TripService {

    @Override
    public List<Trip> searchTrips(int fromStationId, int toStationId, LocalDate date) {
        throw new UnsupportedOperationException("TODO BE2");
    }

    @Override
    public List<Seat> getAvailableSeats(int tripId) {
        throw new UnsupportedOperationException("TODO BE2");
    }

    @Override
    public boolean isSeatAvailable(int tripId, int seatId) {
        throw new UnsupportedOperationException("TODO BE2");
    }
}
