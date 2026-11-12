package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Trip;
import java.time.LocalDateTime;

public record AdminTripDTO(Integer id, String tripCode, String routeName, String waterbusName,
                           LocalDateTime departureTime, LocalDateTime arrivalTime, String status,
                           long soldTickets) {
    public AdminTripDTO(Integer id, String tripCode, String routeName, String waterbusName,
                        LocalDateTime departureTime, LocalDateTime arrivalTime, String status) {
        this(id, tripCode, routeName, waterbusName, departureTime, arrivalTime, status, 0);
    }

    public static AdminTripDTO fromEntity(Trip trip) {
        return fromEntity(trip, 0);
    }

    public static AdminTripDTO fromEntity(Trip trip, long soldTickets) {
        return new AdminTripDTO(trip.getId(), trip.getTripCode(), trip.getRoute().getName(),
                trip.getWaterbus().getName(), trip.getDepartureTime(), trip.getArrivalTime(),
                trip.getStatus(), soldTickets);
    }

    public boolean isCancelable() {
        return "SCHEDULED".equals(status) && departureTime != null
                && departureTime.isAfter(LocalDateTime.now());
    }
}
