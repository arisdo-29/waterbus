package com.hsf302.waterbus.job;

import com.hsf302.waterbus.service.WaterbusService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WaterbusStatusJob {
    private final WaterbusService waterbusService;

    public WaterbusStatusJob(WaterbusService waterbusService) {
        this.waterbusService = waterbusService;
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void updateStatuses() {
        waterbusService.applyScheduledStatuses();
    }
}
