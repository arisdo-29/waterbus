package com.hsf302.waterbus.dto;

import com.hsf302.waterbus.entity.Account;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class AccountDTO implements Serializable {
    private final Integer id;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String role;
    private final String status;
    private final LocalDateTime createdAt;

    public AccountDTO(Integer id, String email, String fullName, String phone, String role, String status) {
        this(id, email, fullName, phone, role, status, null);
    }

    public AccountDTO(Integer id, String email, String fullName, String phone, String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static AccountDTO fromEntity(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountDTO(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public Account toEntity() {
        Account account = new Account();
        account.setId(this.id);
        account.setEmail(this.email);
        account.setFullName(this.fullName);
        account.setPhone(this.phone);
        if (this.role != null) {
            account.setRole(this.role);
        }
        if (this.status != null) {
            account.setStatus(this.status);
        }
        if (this.createdAt != null) {
            account.setCreatedAt(this.createdAt);
        }
        return account;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
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
