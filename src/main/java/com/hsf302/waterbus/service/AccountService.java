package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RegisterRequest;
import com.hsf302.waterbus.entity.Account;
import org.springframework.data.domain.Page;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface AccountService {
    /** Trả về AccountDTO nếu đúng email + mật khẩu (so bằng BCrypt), sai hoặc bị khoá thì trả null. */
    AccountDTO login(String email, String rawPassword);

    /** Tạo tài khoản CUSTOMER từ RegisterRequest, mật khẩu phải mã hoá BCrypt trước khi lưu. */
    AccountDTO register(RegisterRequest request);

    /** Tạo tài khoản CUSTOMER với các tham số lẻ. */
    AccountDTO register(String email, String rawPassword, String fullName, String phone);

    /** Trả về AccountDTO theo ID phục vụ tầng Controller. */
    AccountDTO getAccountById(int id);

    /** Trả về Entity Account phục vụ nội bộ giữa các Service. */
    Account getAccountEntityById(int id);

    /** Trả về AccountDTO theo Email phục vụ tầng Controller. */
    AccountDTO getAccountByEmail(String email);

    /** Kiểm tra trạng thái tài khoản có bị khoá hay không (phục vụ Interceptor). */
    boolean isLocked(int accountId);

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
