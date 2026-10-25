package com.hsf302.waterbus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

// Module: BE4. Dữ liệu form ở trang checkout gửi lên POST /booking.
// Tên field = name của input trong templates/booking/checkout.html (theo docs/PAGES.md), Spring tự điền vào.
public class BookingForm {
    // 4 ô ẩn: chuyến, loại vé, bến đi, bến đến (Controller tự kiểm tra null)
    private Integer tripId;
    private Integer ticketTypeId;
    private Integer fromStationId;
    private Integer toStationId;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 ghế.")
    private List<Integer> seatIds;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
    private String contactName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.")
    private String contactPhone;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 100, message = "Email tối đa 100 ký tự.")
    private String contactEmail;

    @Size(max = 30, message = "Mã khuyến mãi tối đa 30 ký tự.")
    private String promoCode;

    public Integer getTripId() { return tripId; }
    public void setTripId(Integer tripId) { this.tripId = tripId; }
    public Integer getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Integer ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    public Integer getFromStationId() { return fromStationId; }
    public void setFromStationId(Integer fromStationId) { this.fromStationId = fromStationId; }
    public Integer getToStationId() { return toStationId; }
    public void setToStationId(Integer toStationId) { this.toStationId = toStationId; }
    public List<Integer> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Integer> seatIds) { this.seatIds = seatIds; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = clean(contactName); }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = clean(contactPhone); }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = clean(contactEmail); }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = clean(promoCode); }

    // Bỏ khoảng trắng 2 đầu; ô để trống -> null.
    // Nhờ vậy ô SĐT trống chỉ báo "Vui lòng nhập số điện thoại." (@Pattern bỏ qua null), không báo thêm lỗi định dạng.
    private static String clean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
