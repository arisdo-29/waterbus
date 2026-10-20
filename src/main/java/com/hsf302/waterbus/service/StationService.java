package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.StationDTO;

import java.util.List;

// Module: BE1 - Tuyến & Bến
public interface StationService {
    List<StationDTO> getStations();

    List<StationDTO> getActiveStations();

    StationDTO getStationById(int stationId);

    // TODO BE1: addStation, updateStation, deleteStation (ẩn bến: isActive = false) ...
}
