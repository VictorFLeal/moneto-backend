package com.moneto.repository;

import com.moneto.entity.RecurringIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringIncomeRepository extends JpaRepository<RecurringIncome, Long> {

    List<RecurringIncome> findByUserIdOrderByIdDesc(Long userId);

    List<RecurringIncome> findByUserIdAndAtivoTrue(Long userId);
}