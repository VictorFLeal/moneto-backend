package com.moneto.repository;

import com.moneto.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByDataDesc(Long userId);

    List<Transaction> findByUserIdAndTipo(Long userId, String tipo);

    List<Transaction> findByUserIdAndCategoria(Long userId, String categoria);

    List<Transaction> findByUserIdAndDataBetweenOrderByDataDesc(
            Long userId, LocalDate inicio, LocalDate fim
    );

    @Query("SELECT SUM(t.valor) FROM Transaction t WHERE t.user.id = :userId AND t.tipo = :tipo")
    Double sumByUserIdAndTipo(@Param("userId") Long userId, @Param("tipo") String tipo);
}