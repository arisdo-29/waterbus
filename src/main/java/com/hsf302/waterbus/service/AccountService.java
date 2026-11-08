package com.hsf302.waterbus.service;

import com.hsf302.waterbus.entity.Account;
import org.springframework.data.domain.Page;
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

    /** Cập nhật thông tin cá nhân. */
    AccountDTO updateProfile(int accountId, String fullName, String phone);

    /** Đổi mật khẩu tài khoản. */
    void changePassword(int accountId, String currentPassword, String newPassword);

    /** Quản lý tài khoản: Tìm kiếm, lọc và phân trang */
    Page<AccountDTO> getAccounts(String keyword, String role, String status, int page, int size);

    /** Quản lý tài khoản: Tạo tài khoản STAFF mới */
    AccountDTO createStaffAccount(String email, String rawPassword, String fullName, String phone);

    /** Quản lý tài khoản: Thay đổi vai trò (chuyển đổi CUSTOMER <-> STAFF) */
    AccountDTO changeRole(int targetAccountId, String newRole, int currentAdminId);

    /** Quản lý tài khoản: Khoá / Mở khoá tài khoản */
    AccountDTO toggleStatus(int targetAccountId, int currentAdminId);

    /** Quản lý tài khoản: Đặt lại mật khẩu */
    void resetPassword(int targetAccountId, String newPassword);
}
