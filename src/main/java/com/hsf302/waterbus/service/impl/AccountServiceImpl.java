package com.hsf302.waterbus.service.impl;

import com.hsf302.waterbus.entity.Account;
import com.hsf302.waterbus.service.AccountService;
import java.util.List;
import org.springframework.stereotype.Service;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi. TODO: thay các dòng throw bằng code thật.
@Service
public class AccountServiceImpl implements AccountService {

    @Override
    public Account login(String email, String rawPassword) {
        throw new UnsupportedOperationException("TODO BE3");
    }

    @Override
    public Account register(String email, String rawPassword, String fullName, String phone) {
        throw new UnsupportedOperationException("TODO BE3");
    }
}
