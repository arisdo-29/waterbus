package com.hsf302.waterbus.repository;

import com.hsf302.waterbus.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(String role);

    @Query("""
        SELECT a FROM Account a
        WHERE a.id <> 0
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:role IS NULL OR :role = '' OR a.role = :role)
          AND (:status IS NULL OR :status = '' OR a.status = :status)
        ORDER BY a.id DESC
    """)
    Page<Account> searchAccounts(@Param("keyword") String keyword,
                                 @Param("role") String role,
                                 @Param("status") String status,
                                 Pageable pageable);
}
