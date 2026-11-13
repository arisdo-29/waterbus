package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Ticket;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class TicketDTO implements Serializable {
    private final String ticketCode;
    private final String seatCode;
    private final String fromStation;
    private final String toStation;
    private final BigDecimal price;
    private final String status;
    private final String passengerName;
    private final Integer birthYear;
    private final boolean requiresVerification;

    public TicketDTO(String ticketCode, String seatCode, String fromStation, String toStation,
                     BigDecimal price, String status,
                     String passengerName, Integer birthYear, boolean requiresVerification) {
        this.ticketCode = ticketCode;
        this.seatCode = seatCode;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.price = price;
        this.status = status;
        this.passengerName = passengerName;
        this.birthYear = birthYear;
        this.requiresVerification = requiresVerification;
    }

    public static TicketDTO fromEntity(Ticket t, String displayStatus) {
        if (t == null) {
            return null;
        }
        boolean requiresVerification = Boolean.TRUE.equals(t.getTicketType().getRequiresVerification());
        return new TicketDTO(t.getTicketCode(), t.getSeat().getSeatCode(), t.getFromStation().getName(),
                t.getToStation().getName(), t.getPrice(), displayStatus,
                t.getPassengerName(), t.getBirthYear(), requiresVerification);
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public String getFromStation() {
        return fromStation;
    }

    public String getToStation() {
        return toStation;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public boolean isRequiresVerification() {
        return requiresVerification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketDTO that = (TicketDTO) o;
        return Objects.equals(ticketCode, that.ticketCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketCode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(ticketCode = " + ticketCode + ", seatCode = " + seatCode + ", status = " + status + ")";
    }
}
