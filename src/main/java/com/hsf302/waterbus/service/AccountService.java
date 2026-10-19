package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Account;
import java.util.List;

import com.hsf302.waterbus.dto.AccountDTO;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
// Các method có chữ "HỢP ĐỒNG" là module khác sẽ gọi -> muốn đổi chữ ký phải báo nhóm trước.
public interface AccountService {
    /** Trả về Account nếu đúng email + mật khẩu (so bằng BCrypt), sai thì trả null. */
    Account login(String email, String rawPassword);

    /** Tạo tài khoản CUSTOMER, mật khẩu phải mã hoá BCrypt trước khi lưu. */
    Account register(String email, String rawPassword, String fullName, String phone);

    /** Chuyển đổi Entity sang DTO an toàn. */
    AccountDTO toDTO(Account account);

    Account getAccountById(int id);

    Account getAccountByEmail(String email);
}
