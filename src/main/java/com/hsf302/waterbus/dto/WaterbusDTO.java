package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Waterbus;

public record WaterbusDTO(Integer id, String code, String name, Integer capacity, String status) {
    public static WaterbusDTO fromEntity(Waterbus waterbus) {
        return new WaterbusDTO(waterbus.getId(), waterbus.getCode(), waterbus.getName(),
                waterbus.getCapacity(), waterbus.getStatus());
    }
}
