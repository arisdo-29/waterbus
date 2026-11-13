package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.BookingCheckinDTO;
import com.hsf302.waterbus.dto.TicketCheckinDTO;

import java.util.List;

public interface TicketService {
    TicketCheckinDTO getTicketInfo(String ticketCode);

    TicketCheckinDTO getTicketInfoWithLog(String ticketCode, Integer staffAccountId, String source);

    BookingCheckinDTO getBookingCheckinInfo(String bookingCode, Integer staffAccountId, String source);

    java.time.LocalDateTime calculateBoardingTime(String ticketCode);

    boolean isOutsideCheckinWindow(String ticketCode);

    TicketCheckinDTO checkIn(String ticketCode);

    // Giữ tham số cho các bên đang gọi; overrideTripTime không còn cho phép bỏ qua giờ soát vé.
    TicketCheckinDTO checkIn(String ticketCode, Integer staffAccountId, String source, boolean overrideTripTime);

    List<TicketCheckinDTO> checkInGroup(List<String> ticketCodes, Integer staffAccountId, String source);

    TicketCheckinDTO rejectPriorityTicket(String ticketCode, String reason, Integer staffAccountId);

    List<TicketCheckinDTO> getPassengerManifest(int tripId);
}
