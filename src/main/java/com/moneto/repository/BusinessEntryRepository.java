package com.moneto.repository;

import com.moneto.entity.BusinessEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessEntryRepository extends JpaRepository<BusinessEntry, Long> {

    List<BusinessEntry> findByUserIdOrderByDataDesc(Long userId);
}