package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Route;

public record RouteDTO(Integer id, String code, String name, String description, Boolean isActive) {

    public static RouteDTO fromEntity(Route r) {
        return new RouteDTO(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.getIsActive());
    }
}
