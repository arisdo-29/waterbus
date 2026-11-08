package com.hsf302.waterbus.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueDTO(LocalDate date, BigDecimal revenue, long bookings) {
}
