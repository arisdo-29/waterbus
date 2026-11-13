package com.hsf302.waterbus.dto;

public class VnPayResult {
    private final String bookingCode;
    private final boolean success;
    private final String message;

    public VnPayResult(String bookingCode, boolean success, String message) {
        this.bookingCode = bookingCode;
        this.success = success;
        this.message = message;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
