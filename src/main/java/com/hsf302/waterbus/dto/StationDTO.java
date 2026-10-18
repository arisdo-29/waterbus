package com.hsf302.waterbus.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

// VÍ DỤ MẪU (BE1) - viết theo đúng kiểu ArtifactDTO trong project của giảng viên.
public class StationDTO implements Serializable {
    private final Integer id;
    @NotNull
    @Size(max = 20)
    private final String code;
    @NotNull
    @Size(max = 100)
    private final String name;
    @Size(max = 255)
    private final String address;
    private final Boolean isActive;

    public StationDTO(Integer id, String code, String name, String address, Boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.address = address;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDTO that = (StationDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(id = " + id + ", code = " + code + ", name = " + name + ")";
    }
}
