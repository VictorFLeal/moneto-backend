package com.moneto.repository;

import com.moneto.entity.Reserve;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReserveRepository extends JpaRepository<Reserve, Long> {
    List<Reserve> findByUserId(Long userId);
}