package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.entity.Ticket;
import com.hsf302.waterbus.entity.TicketType;
import com.hsf302.waterbus.entity.Trip;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TicketCheckinDTO implements Serializable {
    private final String ticketCode;
    private final String bookingCode;
    private final String passengerName;
    private final String passengerPhone;
    private final String tripCode;
    private final Integer tripId;
    private final String routeName;
    private final String fromStationName;
    private final String toStationName;
    private final LocalDateTime departureTime;
    private final String seatCode;
    private final String waterbusName;
    private final String ticketTypeName;
    private final String ticketTypeCode;
    private final BigDecimal price;
    private final String status;
    private final LocalDateTime checkedInAt;
    private final Integer birthYear;
    private final Integer passengerAge;
    private final Boolean requiresVerification;

    // Full constructor
    public TicketCheckinDTO(String ticketCode, String bookingCode, String passengerName, String passengerPhone,
                            String tripCode, Integer tripId, String routeName, String fromStationName, String toStationName,
                            LocalDateTime departureTime, String seatCode, String waterbusName,
                            String ticketTypeName, String ticketTypeCode, BigDecimal price, String status,
                            LocalDateTime checkedInAt, Integer birthYear, Integer passengerAge, Boolean requiresVerification) {
        this.ticketCode = ticketCode;
        this.bookingCode = bookingCode;
        this.passengerName = passengerName;
        this.passengerPhone = passengerPhone;
        this.tripCode = tripCode;
        this.tripId = tripId;
        this.routeName = routeName;
        this.fromStationName = fromStationName;
        this.toStationName = toStationName;
        this.departureTime = departureTime;
        this.seatCode = seatCode;
        this.waterbusName = waterbusName;
        this.ticketTypeName = ticketTypeName;
        this.ticketTypeCode = ticketTypeCode;
        this.price = price;
        this.status = status;
        this.checkedInAt = checkedInAt;
        this.birthYear = birthYear;
        this.passengerAge = passengerAge;
        this.requiresVerification = requiresVerification != null ? requiresVerification : false;
    }

    // Backward-compatible constructor
    public TicketCheckinDTO(String ticketCode, String bookingCode, String passengerName, String passengerPhone,
                            String tripCode, String routeName, String fromStationName, String toStationName,
                            LocalDateTime departureTime, String seatCode, String waterbusName,
                            String ticketTypeName, BigDecimal price, String status, LocalDateTime checkedInAt) {
        this(ticketCode, bookingCode, passengerName, passengerPhone, tripCode, null, routeName, fromStationName,
                toStationName, departureTime, seatCode, waterbusName, ticketTypeName, null, price, status,
                checkedInAt, null, null, false);
    }

    public static TicketCheckinDTO fromEntity(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        Booking booking = ticket.getBooking();
        Trip trip = ticket.getTrip();
        TicketType ticketType = ticket.getTicketType();

        String routeName = (trip != null && trip.getRoute() != null) ? trip.getRoute().getName() : "";
        String waterbusName = (ticket.getSeat() != null && ticket.getSeat().getWaterbus() != null)
                ? ticket.getSeat().getWaterbus().getName() : "";
        String seatCode = (ticket.getSeat() != null) ? ticket.getSeat().getSeatCode() : "";
        String fromStationName = (ticket.getFromStation() != null) ? ticket.getFromStation().getName() : "";
        String toStationName = (ticket.getToStation() != null) ? ticket.getToStation().getName() : "";
        String ticketTypeName = (ticketType != null) ? ticketType.getName() : "";
        String ticketTypeCode = (ticketType != null) ? ticketType.getCode() : "";
        boolean requiresVerification = (ticketType != null) && Boolean.TRUE.equals(ticketType.getRequiresVerification());

        String bookingCode = (booking != null) ? booking.getBookingCode() : "";
        String passengerName = ticket.getPassengerName();
        if (passengerName == null || passengerName.isBlank()) {
            passengerName = (booking != null) ? booking.getContactName() : "";
        }
        String passengerPhone = (booking != null) ? booking.getContactPhone() : "";
        String tripCode = (trip != null) ? trip.getTripCode() : "";
        Integer tripId = (trip != null) ? trip.getId() : null;
        LocalDateTime departureTime = (trip != null) ? trip.getDepartureTime() : null;

        Integer birthYear = ticket.getBirthYear();
        Integer passengerAge = null;
        if (birthYear != null) {
            passengerAge = LocalDate.now().getYear() - birthYear;
        }

        return new TicketCheckinDTO(
                ticket.getTicketCode(),
                bookingCode,
                passengerName,
                passengerPhone,
                tripCode,
                tripId,
                routeName,
                fromStationName,
                toStationName,
                departureTime,
                seatCode,
                waterbusName,
                ticketTypeName,
                ticketTypeCode,
                ticket.getPrice(),
                ticket.getStatus(),
                ticket.getCheckedInAt(),
                birthYear,
                passengerAge,
                requiresVerification
        );
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getPassengerPhone() {
        return passengerPhone;
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

    public String getSeatCode() {
        return seatCode;
    }

    public String getWaterbusName() {
        return waterbusName;
    }

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public String getTicketTypeCode() {
        return ticketTypeCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public Integer getPassengerAge() {
        return passengerAge;
    }

    public Boolean getRequiresVerification() {
        return requiresVerification;
    }

    public boolean isValid() {
        return "VALID".equalsIgnoreCase(status);
    }

    public boolean isUsed() {
        return "USED".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }
}
