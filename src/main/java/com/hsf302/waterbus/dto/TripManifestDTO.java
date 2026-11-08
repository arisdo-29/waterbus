package com.hsf302.waterbus.dto;

import java.util.List;

public record TripManifestDTO(List<TicketCheckinDTO> passengers, long boardedCount, long totalCount) {
}
