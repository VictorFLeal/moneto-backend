package com.moneto.repository;

import com.moneto.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdOrderByDataDesc(Long userId);

    List<Transaction> findByUserIdAndDataBetweenOrderByDataDesc(Long userId, LocalDate start, LocalDate end);

    // NOVO: conta lançamentos do usuário no mês
    long countByUserIdAndDataBetween(Long userId, LocalDate start, LocalDate end);

    // NOVO: conta lançamentos por origem, ex: "whatsapp"
    long countByUserIdAndOrigemAndDataBetween(Long userId, String origem, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transaction t WHERE t.user.id = :userId AND t.tipo = :tipo")
    Double sumByUserIdAndTipo(@Param("userId") Long userId, @Param("tipo") String tipo);
}