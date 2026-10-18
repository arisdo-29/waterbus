package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
}
