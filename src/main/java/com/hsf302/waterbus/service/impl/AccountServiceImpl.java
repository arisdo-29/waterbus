package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.dto.RegisterRequest;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.repository.AccountRepository;
import com.hsf302.waterbus.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    private Account requireAccount(int id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return null;
        }

        return accountRepository.findByEmailIgnoreCase(email.trim())
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .filter(account -> passwordEncoder.matches(rawPassword, account.getPasswordHash()))
                .map(AccountDTO::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional
    public AccountDTO register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin đăng ký không được để trống");
        }
        return register(request.getEmail(), request.getPassword(), request.getFullName(), request.getPhone());
    }

    @Override
    @Transactional
    public AccountDTO register(String email, String rawPassword, String fullName, String phone) {
        if (email == null || rawPassword == null) {
            throw new IllegalArgumentException("Email và mật khẩu không được để trống");
        }

        String trimmedEmail = email.trim();
        if (accountRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        Account account = new Account();
        account.setEmail(trimmedEmail);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setFullName(fullName != null ? fullName.trim() : "");
        account.setPhone(phone != null ? phone.trim() : null);
        account.setRole("CUSTOMER");
        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);
        return AccountDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(int id) {
        return accountRepository.findById(id)
                .map(AccountDTO::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountEntityById(int id) {
        return accountRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return accountRepository.findByEmailIgnoreCase(email.trim())
                .map(AccountDTO::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(int accountId) {
        return accountRepository.findById(accountId)
                .map(account -> "LOCKED".equalsIgnoreCase(account.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional
    public AccountDTO updateProfile(int accountId, String fullName, String phone) {
        Account account = requireAccount(accountId);

        account.setFullName(fullName != null ? fullName.trim() : "");
        account.setPhone(phone != null ? phone.trim() : null);

        Account saved = accountRepository.save(account);
        return AccountDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public void changePassword(int accountId, String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        Account account = requireAccount(accountId);

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountDTO> getAccounts(String keyword, String role, String status, int page, int size) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role.trim())) ? role.trim().toUpperCase() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim().toUpperCase() : null;

        int pageNum = Math.max(0, page);
        int pageSize = (size > 0) ? size : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Page<Account> accounts = accountRepository.searchAccounts(cleanKeyword, cleanRole, cleanStatus, pageable);
        return accounts.map(AccountDTO::fromEntity);
    }

    @Override
    @Transactional
    public AccountDTO createStaffAccount(String email, String rawPassword, String fullName, String phone) {
        if (email == null || rawPassword == null) {
            throw new IllegalArgumentException("Email và mật khẩu không được để trống.");
        }

        String trimmedEmail = email.trim();
        if (accountRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new IllegalArgumentException("Email [" + trimmedEmail + "] đã được sử dụng trong hệ thống.");
        }

        Account account = new Account();
        account.setEmail(trimmedEmail);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setFullName(fullName != null ? fullName.trim() : "");
        account.setPhone((phone != null && !phone.trim().isEmpty()) ? phone.trim() : null);
        account.setRole("STAFF");
        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);
        return AccountDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public AccountDTO changeRole(int targetAccountId, String newRole, int currentAdminId) {
        if (targetAccountId == SUPER_ADMIN_ID) {
            throw new IllegalArgumentException("Không thể thay đổi vai trò của tài khoản Super Admin!");
        }

        if (targetAccountId == currentAdminId) {
            throw new IllegalArgumentException("Bạn không thể tự thay đổi vai trò của chính mình!");
        }

        Account target = requireAccount(targetAccountId);

        if ("ADMIN".equalsIgnoreCase(target.getRole()) && currentAdminId != SUPER_ADMIN_ID) {
            throw new IllegalArgumentException("Không thể thay đổi vai trò của tài khoản Quản trị viên!");
        }

        if ("ADMIN".equalsIgnoreCase(newRole)) {
            throw new IllegalArgumentException("Hệ thống chỉ cho phép cấp quyền STAFF hoặc CUSTOMER!");
        }

        String upperRole = (newRole != null) ? newRole.trim().toUpperCase() : "";
        if (!"CUSTOMER".equals(upperRole) && !"STAFF".equals(upperRole)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ. Chỉ chấp nhận CUSTOMER hoặc STAFF.");
        }

        target.setRole(upperRole);
        Account saved = accountRepository.save(target);
        return AccountDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public AccountDTO toggleStatus(int targetAccountId, int currentAdminId) {
        if (targetAccountId == SUPER_ADMIN_ID) {
            throw new IllegalArgumentException("Không thể khóa hoặc mở khóa tài khoản Super Admin!");
        }

        if (targetAccountId == currentAdminId) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản của chính mình!");
        }

        Account target = requireAccount(targetAccountId);

        if ("ADMIN".equalsIgnoreCase(target.getRole()) && currentAdminId != SUPER_ADMIN_ID) {
            throw new IllegalArgumentException("Không thể khóa tài khoản Quản trị viên!");
        }

        String nextStatus = "ACTIVE".equalsIgnoreCase(target.getStatus()) ? "LOCKED" : "ACTIVE";
        target.setStatus(nextStatus);

        Account saved = accountRepository.save(target);
        return AccountDTO.fromEntity(saved);
    }

}
