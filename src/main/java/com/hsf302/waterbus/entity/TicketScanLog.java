package com.hsf302.waterbus.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

/**
 * Bảng [ticket_scan_log] - module phụ trách: BE3 - Soát vé.
 * Khớp với database/changes/2026-11-08_db_hanh-khach-soat-ve-hoan-tien.sql
 */
@Entity
@Table(name = "ticket_scan_log")
public class TicketScanLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /** Mã vé nhập/quét được, có thể sai nên không liên kết khoá ngoại. */
    @Size(max = 30)
    @Column(name = "ticket_code", length = 30)
    private String ticketCode;

    @Size(max = 20)
    @Column(name = "booking_code", length = 20)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Account staff;

    /** VIEW, CHECKIN, REJECT */
    @Size(max = 20)
    @NotNull
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    /** SUCCESS, FAILED */
    @Size(max = 10)
    @NotNull
    @Column(name = "result", nullable = false, length = 10)
    private String result;

    /** MANUAL, QR, MANIFEST, OVERRIDE */
    @Size(max = 20)
    @NotNull
    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Size(max = 255)
    @Nationalized
    @Column(name = "message", length = 255)
    private String message;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Account getStaff() {
        return staff;
    }

    public void setStaff(Account staff) {
        this.staff = staff;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
