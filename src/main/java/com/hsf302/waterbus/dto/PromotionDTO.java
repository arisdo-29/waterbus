package com.hsf302.waterbus.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class PromotionDTO implements Serializable {
    private Integer id;

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    @Size(max = 30, message = "Mã tối đa 30 ký tự")
    private String code;

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Loại giảm giá không được để trống")
    private String discountType = "PERCENT"; // PERCENT | FIXED

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    private Integer usageLimit;

    private Integer usedCount = 0;

    private Boolean isActive = true;

    public PromotionDTO() {
    }

    public PromotionDTO(Integer id, String code, String name, String discountType, BigDecimal discountValue,
                        BigDecimal maxDiscount, BigDecimal minOrderAmount, LocalDateTime startAt,
                        LocalDateTime endAt, Integer usageLimit, Integer usedCount, Boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscount = maxDiscount;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        this.startAt = startAt;
        this.endAt = endAt;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount != null ? usedCount : 0;
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

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
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
        PromotionDTO that = (PromotionDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
