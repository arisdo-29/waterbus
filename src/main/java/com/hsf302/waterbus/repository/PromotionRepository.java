package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Module: BE3 - Tài khoản, Loại vé & Khuyến mãi
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    Optional<Promotion> findByCodeIgnoreCase(String code);
    Optional<Promotion> findByCodeIgnoreCaseAndIsActiveTrue(String code);
    List<Promotion> findByIsActiveTrue();
}
