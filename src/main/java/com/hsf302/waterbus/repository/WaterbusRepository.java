package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Waterbus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaterbusRepository extends JpaRepository<Waterbus, Integer> {
    List<Waterbus> findAllByOrderByIdAsc();
    Optional<Waterbus> findFirstByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Waterbus w WHERE w.id = :id")
    Optional<Waterbus> findByIdForUpdate(@Param("id") int id);

    List<Waterbus> findByPendingStatusIsNotNullOrStatusEndsAtIsNotNull();
}
