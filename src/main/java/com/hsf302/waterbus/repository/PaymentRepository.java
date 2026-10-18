package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE4 - Đặt vé & Thanh toán
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}
