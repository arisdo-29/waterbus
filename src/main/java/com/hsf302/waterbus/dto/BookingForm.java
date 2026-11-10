package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.service.BookingService;
import jakarta.validation.constraints.AssertTrue;
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

    // Annotation chỉ nhận hằng số lúc biên dịch: MAX_SEATS_PER_BOOKING là hằng int nên dùng được,
    // ghép chuỗi với hằng đó vẫn là hằng -> đổi con số 1 chỗ thì cả giới hạn lẫn câu báo lỗi cùng đổi
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

    // Vé ưu đãi: mỗi ghế 1 họ tên + 1 năm sinh, cùng thứ tự với seatIds (ghế thứ i <-> phần tử thứ i).
    // Vé thường thì form không gửi 2 ô này -> để null. BookingService.validatePassengers kiểm tra chi tiết.
    private List<String> passengerNames;
    private List<Integer> birthYears;

    // Ô tick "Tôi đã đọc chính sách vé". Dùng boolean (không phải Boolean):
    // không tick thì trình duyệt không gửi gì -> giữ mặc định false -> @AssertTrue báo lỗi.
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
    // Field kiểu boolean thì getter tên là isXxx (Java Bean), Spring vẫn tự hiểu
    public boolean isAcceptPolicy() { return acceptPolicy; }
    public void setAcceptPolicy(boolean acceptPolicy) { this.acceptPolicy = acceptPolicy; }

    // Bỏ khoảng trắng 2 đầu; ô để trống -> null.
    // Nhờ vậy ô SĐT trống chỉ báo "Vui lòng nhập số điện thoại." (@Pattern bỏ qua null), không báo thêm lỗi định dạng.
    private static String clean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }
}
