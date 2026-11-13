package com.hsf302.waterbus.dto;

import java.util.List;

public record TripCancellationPreviewDTO(AdminTripDTO trip, List<TicketCheckinDTO> tickets,
                                         long confirmedTicketCount, long heldTicketCount) {
}
