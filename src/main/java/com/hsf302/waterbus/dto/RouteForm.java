package com.hsf302.waterbus.dto;

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
}
