package com.moneto.repository;

import com.moneto.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByAtivoTrueOrderByOrderAsc();

}