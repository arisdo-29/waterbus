package com.hsf302.waterbus.dto;

import java.time.LocalDateTime;

public record AdminTripDTO(Integer id, String tripCode, String routeName, String waterbusName,
                           LocalDateTime departureTime, LocalDateTime arrivalTime, String status,
                           long soldTickets) {
    public AdminTripDTO(Integer id, String tripCode, String routeName, String waterbusName,
                        LocalDateTime departureTime, LocalDateTime arrivalTime, String status) {
        this(id, tripCode, routeName, waterbusName, departureTime, arrivalTime, status, 0);
    }

    public boolean isCancelable() {
        return "SCHEDULED".equals(status) && departureTime != null
                && departureTime.isAfter(LocalDateTime.now());
    }
}
