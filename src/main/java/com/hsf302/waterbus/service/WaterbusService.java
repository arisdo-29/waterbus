package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.dto.WaterbusDTO;
import java.util.List;

public interface WaterbusService {
    List<Waterbus> getWaterbuses();

    List<WaterbusDTO> getWaterbusViews();

    void saveWaterbus(AdminWaterbusForm form);

    void changeStatus(int waterbusId, String status);

    long countUpcomingScheduledTrips(int waterbusId);
}
