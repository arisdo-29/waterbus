package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Waterbus;
import com.hsf302.waterbus.dto.AdminWaterbusForm;
import com.hsf302.waterbus.dto.WaterbusDTO;
import com.hsf302.waterbus.dto.TripCancellationPreviewDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface WaterbusService {
    List<Waterbus> getWaterbuses();

    List<WaterbusDTO> getWaterbusViews();

    void saveWaterbus(AdminWaterbusForm form);

    void changeStatus(int waterbusId, String status);

    List<TripCancellationPreviewDTO> previewStatusChange(int waterbusId, String status,
                                                           LocalDateTime startsAt, LocalDateTime endsAt);

    int scheduleStatusChange(int waterbusId, String status, LocalDateTime startsAt,
                             LocalDateTime endsAt, List<Integer> previewedTripIds);

    void applyScheduledStatuses();

    long countUpcomingScheduledTrips(int waterbusId);
}
