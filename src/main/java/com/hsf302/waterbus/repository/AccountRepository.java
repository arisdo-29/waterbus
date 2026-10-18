package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface AccountRepository extends JpaRepository<Account, Integer> {
}
