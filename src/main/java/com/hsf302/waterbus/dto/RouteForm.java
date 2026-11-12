package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Route;
import com.hsf302.waterbus.entity.RouteStation;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public class RouteForm {
    @Positive(message = "ID tuyến không hợp lệ.")
    private Integer id;
    @NotBlank(message = "Vui lòng nhập mã tuyến.")
    @Size(max = 20, message = "Mã tuyến tối đa 20 ký tự.")
    private String code;
    @NotBlank(message = "Vui lòng nhập tên tuyến.")
    @Size(max = 100, message = "Tên tuyến tối đa 100 ký tự.")
    private String name;
    @Size(max = 255, message = "Mô tả tối đa 255 ký tự.")
    private String description;
    @NotNull(message = "Vui lòng chọn trạng thái tuyến.")
    private Boolean isActive = true;
    @NotNull(message = "Vui lòng chọn ít nhất hai bến.")
    @Size(min = 2, message = "Tuyến cần ít nhất hai bến khác nhau.")
    private List<@NotNull(message = "Vui lòng chọn bến.") @Positive(message = "ID bến không hợp lệ.") Integer> stationIds = new ArrayList<>();

    @NotNull(message = "Vui lòng nhập số phút cho từng bến.")
    @Size(min = 2, message = "Vui lòng nhập số phút cho ít nhất hai bến.")
    private List<@NotNull(message = "Vui lòng nhập số phút của bến.")
            @Min(value = 0, message = "Số phút không được âm.") Integer> minutesFromStart = new ArrayList<>();

    // Hai danh sách lấy từ cùng thứ tự bến để số phút không bị ghép nhầm.
    public static RouteForm fromEntity(Route r, List<RouteStation> stops) {
        RouteForm form = new RouteForm();
        form.setId(r.getId());
        form.setCode(r.getCode());
        form.setName(r.getName());
        form.setDescription(r.getDescription());
        form.setIsActive(r.getIsActive());
        form.setStationIds(stops.stream().map(stop -> stop.getStation().getId()).toList());
        form.setMinutesFromStart(stops.stream().map(RouteStation::getMinutesFromStart).toList());
        return form;
    }

    // Chép phần thông tin tuyến từ form sang entity (không chép id; danh sách bến do service xử lý)
    public void applyTo(Route r) {
        r.setCode(code);
        r.setName(name);
        r.setDescription(description);
        r.setIsActive(isActive);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code == null ? null : code.strip(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<Integer> getStationIds() { return stationIds; }
    public void setStationIds(List<Integer> stationIds) { this.stationIds = stationIds; }
    public List<Integer> getMinutesFromStart() { return minutesFromStart; }
    public void setMinutesFromStart(List<Integer> minutesFromStart) { this.minutesFromStart = minutesFromStart; }
}
