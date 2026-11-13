package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Booking;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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
    private final LocalDateTime paidAt;
    private final String refundStatus;
    private String promotionCode;

    public BookingDTO(Integer id, String bookingCode, String status, String contactName, String contactPhone,
                      String contactEmail, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount,
                      LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime paidAt) {
        this(id, bookingCode, status, contactName, contactPhone, contactEmail, subtotal, discountAmount,
                totalAmount, createdAt, expiresAt, paidAt, null);
    }

    public BookingDTO(Integer id, String bookingCode, String status, String contactName, String contactPhone,
                      String contactEmail, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal totalAmount,
                      LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime paidAt, String refundStatus) {
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
        this.refundStatus = refundStatus;
    }

    public static BookingDTO fromEntity(Booking b, String displayStatus, String refundStatus) {
        if (b == null) {
            return null;
        }
        BookingDTO dto = new BookingDTO(b.getId(), b.getBookingCode(), displayStatus, b.getContactName(),
                b.getContactPhone(), b.getContactEmail(), b.getSubtotal(), b.getDiscountAmount(),
                b.getTotalAmount(), b.getCreatedAt(), b.getExpiresAt(), b.getPaidAt(), refundStatus);
        dto.promotionCode = b.getPromotion() != null ? b.getPromotion().getCode() : null;
        return dto;
    }

    public String getPromotionCode() {
        return promotionCode;
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

    public String getRefundStatus() {
        return refundStatus;
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
