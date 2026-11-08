package com.hsf302.waterbus.dto;

import java.math.BigDecimal;

public record RevenueSummaryDTO(BigDecimal totalRevenue, long paidBookings, long ticketsSold,
                                long freeTickets, BigDecimal totalDiscount, BigDecimal totalRefunded,
                                long cancelledOrExpiredBookings) {
}
