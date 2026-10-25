package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.TicketCheckinDTO;
import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.Trip;
import com.hsf302.waterbus.repository.TicketRepository;
import com.hsf302.waterbus.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

// Module: BE3 (Tân) - Soát vé (Staff Check-in) theo M4
@Service
public class TicketServiceImpl implements TicketService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final TicketRepository ticketRepository;

    // Constructor Injection
    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketCheckinDTO getTicketInfo(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        return mapToDTO(ticket);
    }

    @Override
    @Transactional
    public TicketCheckinDTO checkIn(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);
        String currentStatus = ticket.getStatus();

        if ("USED".equalsIgnoreCase(currentStatus)) {
            String timeStr = (ticket.getCheckedInAt() != null)
                    ? ticket.getCheckedInAt().format(TIME_FORMAT)
                    : "trước đó";
            throw new IllegalStateException("Vé " + ticket.getTicketCode() + " đã được soát lúc " + timeStr + ", không thể tái sử dụng!");
        }

        if ("HOLD".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Vé " + ticket.getTicketCode() + " chưa được thanh toán (trạng thái: Đang giữ chỗ)!");
        }

        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Vé " + ticket.getTicketCode() + " đã bị hủy, không hợp lệ để lên tàu!");
        }

        if (!"VALID".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Vé " + ticket.getTicketCode() + " không hợp lệ để soát (trạng thái: " + currentStatus + ")!");
        }

        // Cập nhật trạng thái vé sang USED và đóng dấu thời gian soát vé
        ticket.setStatus("USED");
        ticket.setCheckedInAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        Ticket saved = ticketRepository.save(ticket);

        return mapToDTO(saved);
    }

    private Ticket findTicketOrThrow(String ticketCode) {
        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã vé cần soát.");
        }
        String trimmed = ticketCode.trim();
        return ticketRepository.findByTicketCode(trimmed)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé với mã: " + trimmed));
    }

    private TicketCheckinDTO mapToDTO(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = ticket.getTrip();

        String routeName = (trip != null && trip.getRoute() != null) ? trip.getRoute().getName() : "";
        String waterbusName = (ticket.getSeat() != null && ticket.getSeat().getWaterbus() != null)
                ? ticket.getSeat().getWaterbus().getName() : "";
        String seatCode = (ticket.getSeat() != null) ? ticket.getSeat().getSeatCode() : "";
        String fromStationName = (ticket.getFromStation() != null) ? ticket.getFromStation().getName() : "";
        String toStationName = (ticket.getToStation() != null) ? ticket.getToStation().getName() : "";
        String ticketTypeName = (ticket.getTicketType() != null) ? ticket.getTicketType().getName() : "";
        String bookingCode = (booking != null) ? booking.getBookingCode() : "";
        String passengerName = (booking != null) ? booking.getContactName() : "";
        String passengerPhone = (booking != null) ? booking.getContactPhone() : "";
        String tripCode = (trip != null) ? trip.getTripCode() : "";
        LocalDateTime departureTime = (trip != null) ? trip.getDepartureTime() : null;

        return new TicketCheckinDTO(
                ticket.getTicketCode(),
                bookingCode,
                passengerName,
                passengerPhone,
                tripCode,
                routeName,
                fromStationName,
                toStationName,
                departureTime,
                seatCode,
                waterbusName,
                ticketTypeName,
                ticket.getPrice(),
                ticket.getStatus(),
                ticket.getCheckedInAt()
        );
    }
}
