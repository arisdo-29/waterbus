package com.hsf302.waterbus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class TicketTypeDTO implements Serializable {
    private Integer id;

    @NotBlank(message = "Mã loại vé không được để trống")
    @Size(max = 30, message = "Mã loại vé tối đa 30 ký tự")
    private String code;

    @NotBlank(message = "Tên loại vé không được để trống")
    @Size(max = 100, message = "Tên loại vé tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0", message = "Giá vé phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    private Boolean requiresVerification = false;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    private Boolean isActive = true;

    public TicketTypeDTO() {
    }

    public TicketTypeDTO(Integer id, String code, String name, BigDecimal price,
                         Boolean requiresVerification, String description, Boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.price = price;
        this.requiresVerification = requiresVerification != null ? requiresVerification : false;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getRequiresVerification() {
        return requiresVerification;
    }

    public void setRequiresVerification(Boolean requiresVerification) {
        this.requiresVerification = requiresVerification;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketTypeDTO that = (TicketTypeDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
