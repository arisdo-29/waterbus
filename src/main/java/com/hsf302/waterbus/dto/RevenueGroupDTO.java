package com.hsf302.waterbus.dto;

import java.math.BigDecimal;

public record RevenueGroupDTO(String name, long bookings, long tickets, BigDecimal revenue) {
}
