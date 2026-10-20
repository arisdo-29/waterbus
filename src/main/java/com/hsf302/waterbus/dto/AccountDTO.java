package com.hsf302.waterbus.dto;

import java.io.Serializable;
import java.util.Objects;

// DTO chuyển dữ liệu Account ra ngoài View và lưu vào Session "loginAccount"
public class AccountDTO implements Serializable {
    private final Integer id;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String role;
    private final String status;

    public AccountDTO(Integer id, String email, String fullName, String phone, String role, String status) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountDTO that = (AccountDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "AccountDTO(id=" + id + ", email=" + email + ", fullName=" + fullName + ", role=" + role + ")";
    }
}
