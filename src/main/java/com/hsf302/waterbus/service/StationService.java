package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.StationDTO;
import com.hsf302.waterbus.dto.StationForm;

import java.util.List;

// Module: BE1 - Tuyến & Bến
public interface StationService {
    List<StationDTO> getStations();

    List<StationDTO> getActiveStations();

    StationDTO getStationById(int stationId);

    StationForm getStationForEdit(int stationId);

    int saveStation(StationForm form);

    void hideStation(int stationId);

}
