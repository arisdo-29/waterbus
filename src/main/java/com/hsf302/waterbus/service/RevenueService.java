package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.RevenueReportDTO;
import java.time.LocalDate;

public interface RevenueService {
    RevenueReportDTO getReport(LocalDate from, LocalDate to);
}
