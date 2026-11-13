package com.hsf302.waterbus.service;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RegisterRequest;
import com.hsf302.waterbus.entity.Account;
import org.springframework.data.domain.Page;

public interface AccountService {
    int SUPER_ADMIN_ID = 0;

    AccountDTO login(String email, String rawPassword);

    AccountDTO register(RegisterRequest request);

    AccountDTO register(String email, String rawPassword, String fullName, String phone);

    AccountDTO getAccountById(int id);

    Account getAccountEntityById(int id);

    AccountDTO getAccountByEmail(String email);

    boolean isLocked(int accountId);

    AccountDTO updateProfile(int accountId, String fullName, String phone);

    void changePassword(int accountId, String currentPassword, String newPassword);

    Page<AccountDTO> getAccounts(String keyword, String role, String status, int page, int size);

    AccountDTO createStaffAccount(String email, String rawPassword, String fullName, String phone);

    AccountDTO changeRole(int targetAccountId, String newRole, int currentAdminId);

    AccountDTO toggleStatus(int targetAccountId, int currentAdminId);

    void resetPassword(int targetAccountId, String newPassword);
}
