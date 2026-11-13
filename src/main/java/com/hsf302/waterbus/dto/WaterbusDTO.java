package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Waterbus;
import java.time.LocalDateTime;

public record WaterbusDTO(Integer id, String code, String name, Integer capacity, String status,
                          String pendingStatus, LocalDateTime statusStartsAt, LocalDateTime statusEndsAt) {
    public static WaterbusDTO fromEntity(Waterbus waterbus) {
        return new WaterbusDTO(waterbus.getId(), waterbus.getCode(), waterbus.getName(),
                waterbus.getCapacity(), waterbus.getStatus(), waterbus.getPendingStatus(),
                waterbus.getStatusStartsAt(), waterbus.getStatusEndsAt());
    }
}
