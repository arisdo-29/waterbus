package com.hsf302.waterbus.dto;

import java.time.LocalDateTime;

public record AdminTripDTO(Integer id, String tripCode, String routeName, String waterbusName,
                           LocalDateTime departureTime, LocalDateTime arrivalTime, String status) {
}
