package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Station;
import jakarta.validation.constraints.*;

/** Only these fields may be submitted by the station admin form. */
public class StationForm {
    @Positive(message = "ID bến không hợp lệ.")
    private Integer id;
    @NotBlank(message = "Vui lòng nhập mã bến.")
    @Size(max = 20, message = "Mã bến tối đa 20 ký tự.")
    private String code;
    @NotBlank(message = "Vui lòng nhập tên bến.")
    @Size(max = 100, message = "Tên bến tối đa 100 ký tự.")
    private String name;
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự.")
    private String address;
    @NotNull(message = "Vui lòng chọn trạng thái bến.")
    private Boolean isActive = true;

    // Đổ dữ liệu bến có sẵn vào form để mở trang sửa
    public static StationForm fromEntity(Station s) {
        StationForm form = new StationForm();
        form.setId(s.getId());
        form.setCode(s.getCode());
        form.setName(s.getName());
        form.setAddress(s.getAddress());
        form.setIsActive(s.getIsActive());
        return form;
    }

    // Chép các field được phép sửa từ form sang entity (không chép id)
    public void applyTo(Station s) {
        s.setCode(code);
        s.setName(name);
        s.setAddress(address);
        s.setIsActive(isActive);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code == null ? null : code.strip(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
