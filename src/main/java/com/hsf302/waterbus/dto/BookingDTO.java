package com.hsf302.waterbus.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// Module: BE4 - Đặt vé & Thanh toán. Thông tin 1 đơn đặt vé đưa ra view (attribute "booking").
// Viết theo kiểu StationDTO. Tên field khớp docs/PAGES.md.
public class BookingDTO implements Serializable {
    private final Integer id;
    private final String bookingCode;
    private final String status;
    private final String contactName;
    private final String contactPhone;
    private final String contactEmail;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime paidAt; // null khi chưa thanh toán

    public BookingDTO(Integer id, String bookingCode, String status, String contactName, String contactPhone,
                      String contactEmail, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount,
                      LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime paidAt) {
        this.id = id;
        this.bookingCode = bookingCode;
        this.status = status;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.paidAt = paidAt;
    }

    public Integer getId() {
        return id;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getStatus() {
        return status;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingDTO that = (BookingDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(bookingCode, that.bookingCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookingCode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(id = " + id + ", bookingCode = " + bookingCode + ", status = " + status + ")";
    }
}
