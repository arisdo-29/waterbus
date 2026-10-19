package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
