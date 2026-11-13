package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Booking;
import com.hsf302.waterbus.service.BookingService;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BookingForm {
    private Integer tripId;
    private Integer ticketTypeId;
    private Integer fromStationId;
    private Integer toStationId;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 ghế.")
    @Size(max = BookingService.MAX_SEATS_PER_BOOKING,
            message = "Bạn chỉ có thể chọn tối đa " + BookingService.MAX_SEATS_PER_BOOKING + " ghế trong một lần đặt.")
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

    private List<String> passengerNames;
    private List<Integer> birthYears;

    @AssertTrue(message = "Vui lòng xác nhận đã đọc chính sách vé.")
    private boolean acceptPolicy;

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
    public List<String> getPassengerNames() { return passengerNames; }
    public void setPassengerNames(List<String> passengerNames) { this.passengerNames = passengerNames; }
    public List<Integer> getBirthYears() { return birthYears; }
    public void setBirthYears(List<Integer> birthYears) { this.birthYears = birthYears; }
    public boolean isAcceptPolicy() { return acceptPolicy; }
    public void setAcceptPolicy(boolean acceptPolicy) { this.acceptPolicy = acceptPolicy; }

    public Booking toEntity() {
        Booking booking = new Booking();
        booking.setContactName(clean(contactName));
        booking.setContactPhone(clean(contactPhone));
        booking.setContactEmail(clean(contactEmail));
        return booking;
    }

    private static String clean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
