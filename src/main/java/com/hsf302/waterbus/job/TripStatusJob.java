package com.hsf302.waterbus.job;

import com.hsf302.waterbus.repository.TripRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
public class TripStatusJob {
    private final TripRepository tripRepository;

    public TripStatusJob(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void updateTripStatuses() {
        LocalDateTime now = LocalDateTime.now();
        tripRepository.completeArrivedTrips(now);
        tripRepository.departStartedTrips(now);
    }
}
