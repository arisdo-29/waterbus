package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Seat;
import java.io.Serializable;

public class SeatDTO implements Serializable {
    private final Integer id;
    private final String seatCode;
    private final Integer seatRow;
    private final Integer seatCol;
    private final boolean available;

    public SeatDTO(Integer id, String seatCode, Integer seatRow, Integer seatCol, boolean available) {
        this.id = id;
        this.seatCode = seatCode;
        this.seatRow = seatRow;
        this.seatCol = seatCol;
        this.available = available;
    }

    public static SeatDTO fromEntity(Seat seat, boolean available) {
        return new SeatDTO(seat.getId(), seat.getSeatCode(), seat.getSeatRow(), seat.getSeatCol(), available);
    }

    public Integer getId() {
        return id;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public Integer getSeatRow() {
        return seatRow;
    }

    public Integer getSeatCol() {
        return seatCol;
    }

    public boolean isAvailable() {
        return available;
    }
}
