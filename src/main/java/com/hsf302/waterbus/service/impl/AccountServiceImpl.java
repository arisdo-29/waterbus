package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.dto.AccountDTO;
import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.repository.AccountRepository;
import com.hsf302.waterbus.service.AccountService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi.
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public Account login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return null;
        }

        return accountRepository.findByEmailIgnoreCase(email.trim())
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .filter(account -> passwordEncoder.matches(rawPassword, account.getPasswordHash()))
                .orElse(null);
    }

    @Override
    @Transactional
    public Account register(String email, String rawPassword, String fullName, String phone) {
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

        return accountRepository.save(account);
    }

    @Override
    public AccountDTO toDTO(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountDTO(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getRole(),
                account.getStatus()
        );
    }

    @Override
    public Account getAccountById(int id) {
        return accountRepository.findById(id).orElse(null);
    }

    @Override
    public Account getAccountByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return accountRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
    }

    @Override
    @Transactional
    public AccountDTO updateProfile(int accountId, String fullName, String phone) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        account.setFullName(fullName != null ? fullName.trim() : "");
        account.setPhone(phone != null ? phone.trim() : null);

        Account saved = accountRepository.save(account);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void changePassword(int accountId, String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }
}
