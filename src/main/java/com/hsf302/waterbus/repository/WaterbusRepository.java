package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Waterbus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WaterbusRepository extends JpaRepository<Waterbus, Integer> {
    List<Waterbus> findAllByOrderByIdAsc();
    Optional<Waterbus> findFirstByCodeIgnoreCase(String code);
}
