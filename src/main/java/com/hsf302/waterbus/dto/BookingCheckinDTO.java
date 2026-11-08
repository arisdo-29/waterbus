package com.hsf302.waterbus.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chứa thông tin đơn và danh sách vé để soát theo nhóm/đơn (Group Check-in).
 */
public class BookingCheckinDTO implements Serializable {
    private final String bookingCode;
    private final String contactName;
    private final String contactPhone;
    private final String tripCode;
    private final Integer tripId;
    private final String routeName;
    private final String fromStationName;
    private final String toStationName;
    private final LocalDateTime departureTime;
    private final String status;
    private final List<TicketCheckinDTO> tickets;

    public BookingCheckinDTO(String bookingCode, String contactName, String contactPhone,
                             String tripCode, Integer tripId, String routeName,
                             String fromStationName, String toStationName, LocalDateTime departureTime,
                             String status, List<TicketCheckinDTO> tickets) {
        this.bookingCode = bookingCode;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.tripCode = tripCode;
        this.tripId = tripId;
        this.routeName = routeName;
        this.fromStationName = fromStationName;
        this.toStationName = toStationName;
        this.departureTime = departureTime;
        this.status = status;
        this.tickets = tickets;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getTripCode() {
        return tripCode;
    }

    public Integer getTripId() {
        return tripId;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getFromStationName() {
        return fromStationName;
    }

    public String getToStationName() {
        return toStationName;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public String getStatus() {
        return status;
    }

    public List<TicketCheckinDTO> getTickets() {
        return tickets;
    }

    public long getValidCount() {
        return tickets == null ? 0 : tickets.stream().filter(TicketCheckinDTO::isValid).count();
    }

    public long getUsedCount() {
        return tickets == null ? 0 : tickets.stream().filter(TicketCheckinDTO::isUsed).count();
    }

    public int getTotalCount() {
        return tickets == null ? 0 : tickets.size();
    }
}

