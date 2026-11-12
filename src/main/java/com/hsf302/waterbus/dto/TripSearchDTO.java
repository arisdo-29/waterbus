package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDateTime;

public class TripSearchDTO {
    private final Integer id;
    private final String tripCode;
    private final String routeName;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final long availableSeats;

    public TripSearchDTO(Integer id, String tripCode, String routeName,
                         LocalDateTime departureTime, LocalDateTime arrivalTime, long availableSeats) {
        this.id = id;
        this.tripCode = tripCode;
        this.routeName = routeName;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.availableSeats = availableSeats;
    }

    public static TripSearchDTO forSegment(Trip trip, int departureMinutes, int arrivalMinutes,
                                           long availableSeats) {
        return new TripSearchDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                trip.getDepartureTime().plusMinutes(departureMinutes),
                trip.getDepartureTime().plusMinutes(arrivalMinutes), availableSeats);
    }

    public Integer getId() {
        return id;
    }

    public String getTripCode() {
        return tripCode;
    }

    public String getRouteName() {
        return routeName;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public long getAvailableSeats() {
        return availableSeats;
    }
}
