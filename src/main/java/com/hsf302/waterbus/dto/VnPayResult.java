package com.hsf302.waterbus.dto;

// Module: BE4 - Đặt vé & Thanh toán. Kết quả xử lý khi VNPay trả khách về (GET /payment/vnpay-return).
// bookingCode = null khi không xác định được đơn (chữ ký sai, sai mã đơn) -> Controller đưa khách về trang chủ.
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
