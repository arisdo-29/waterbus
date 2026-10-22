package com.hsf302.waterbus.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO chứa thông tin chi tiết vé dùng cho luồng Soát vé (Staff Check-in)
public class TicketCheckinDTO implements Serializable {
    private final String ticketCode;
    private final String bookingCode;
    private final String passengerName;
    private final String passengerPhone;
    private final String tripCode;
    private final String routeName;
    private final String fromStationName;
    private final String toStationName;
    private final LocalDateTime departureTime;
    private final String seatCode;
    private final String waterbusName;
    private final String ticketTypeName;
    private final BigDecimal price;
    private final String status;
    private final LocalDateTime checkedInAt;

    public TicketCheckinDTO(String ticketCode, String bookingCode, String passengerName, String passengerPhone,
                            String tripCode, String routeName, String fromStationName, String toStationName,
                            LocalDateTime departureTime, String seatCode, String waterbusName,
                            String ticketTypeName, BigDecimal price, String status, LocalDateTime checkedInAt) {
        this.ticketCode = ticketCode;
        this.bookingCode = bookingCode;
        this.passengerName = passengerName;
        this.passengerPhone = passengerPhone;
        this.tripCode = tripCode;
        this.routeName = routeName;
        this.fromStationName = fromStationName;
        this.toStationName = toStationName;
        this.departureTime = departureTime;
        this.seatCode = seatCode;
        this.waterbusName = waterbusName;
        this.ticketTypeName = ticketTypeName;
        this.price = price;
        this.status = status;
        this.checkedInAt = checkedInAt;
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

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
}
