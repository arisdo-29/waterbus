package com.hsf302.waterbus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

// DTO tạo tài khoản nhân viên (STAFF) bởi Admin
public class CreateStaffRequest implements Serializable {

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 100, message = "Email tối đa 100 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
    private String fullName;

    @Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu ban đầu.")
    @Size(min = 6, max = 50, message = "Mật khẩu từ 6 đến 50 ký tự.")
    private String password;

    public CreateStaffRequest() {
    }

    public CreateStaffRequest(String email, String fullName, String phone, String password) {
        this.email = email;
        this.fullName = fullName;
        this.phone = (phone == null || phone.trim().isEmpty()) ? null : phone.trim();
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = (phone == null || phone.trim().isEmpty()) ? null : phone.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
