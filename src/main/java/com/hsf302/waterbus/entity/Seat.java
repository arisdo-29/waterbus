package com.hsf302.waterbus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Bảng [seat] - module phụ trách: BE2 - Tàu, Ghế & Chuyến. Khớp với database/WaterbusDB_SQLServer.sql */
@Entity
@Table(name = "seat")
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waterbus_id", nullable = false)
    private Waterbus waterbus;

    @Size(max = 5)
    @NotNull
    @Column(name = "seat_code", nullable = false, length = 5)
    private String seatCode;

    @NotNull
    @Column(name = "seat_row", nullable = false)
    private Integer seatRow;

    @NotNull
    @Column(name = "seat_col", nullable = false)
    private Integer seatCol;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Waterbus getWaterbus() {
        return waterbus;
    }

    public void setWaterbus(Waterbus waterbus) {
        this.waterbus = waterbus;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public Integer getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(Integer seatRow) {
        this.seatRow = seatRow;
    }

    public Integer getSeatCol() {
        return seatCol;
    }

    public void setSeatCol(Integer seatCol) {
        this.seatCol = seatCol;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
