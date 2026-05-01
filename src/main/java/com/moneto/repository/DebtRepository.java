package com.moneto.repository;

import com.moneto.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByUserId(Long userId);
    List<Debt> findByUserIdAndStatus(Long userId, String status);
}