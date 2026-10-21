package com.hsf302.waterbus.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

// Module: BE4 - Đặt vé & Thanh toán. Thông tin 1 vé đưa ra view (danh sách "tickets").
// fromStation / toStation là TÊN bến (chuỗi), không phải object - theo docs/PAGES.md.
public class TicketDTO implements Serializable {
    private final String ticketCode;
    private final String seatCode;
    private final String fromStation;
    private final String toStation;
    private final BigDecimal price;
    private final String status;

    public TicketDTO(String ticketCode, String seatCode, String fromStation, String toStation,
                     BigDecimal price, String status) {
        this.ticketCode = ticketCode;
        this.seatCode = seatCode;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.price = price;
        this.status = status;
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
