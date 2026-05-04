package com.moneto.repository;

import com.moneto.entity.ReserveMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReserveMovementRepository extends JpaRepository<ReserveMovement, Long> {
    List<ReserveMovement> findByReserveId(Long reserveId);
}