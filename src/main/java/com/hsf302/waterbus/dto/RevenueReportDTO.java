package com.hsf302.waterbus.dto;

import java.util.List;

public record RevenueReportDTO(RevenueSummaryDTO summary, List<DailyRevenueDTO> dailyRevenue,
                               List<RevenueGroupDTO> byRoute, List<RevenueGroupDTO> byTicketType,
                               List<RevenueGroupDTO> byMethod, List<RevenueGroupDTO> byPromotion) {
}
