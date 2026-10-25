package com.hsf302.waterbus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AdminWaterbusForm {
    private Integer id;

    @NotBlank(message = "Vui lòng nhập mã tàu.")
    @Size(max = 20, message = "Mã tàu tối đa 20 ký tự.")
    private String code;

    @NotBlank(message = "Vui lòng nhập tên tàu.")
    @Size(max = 100, message = "Tên tàu tối đa 100 ký tự.")
    private String name;

    @NotNull(message = "Vui lòng nhập sức chứa.")
    @Positive(message = "Sức chứa phải lớn hơn 0.")
    private Integer capacity;

    @NotBlank(message = "Vui lòng chọn trạng thái.")
    private String status = "ACTIVE";

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
