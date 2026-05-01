package com.moneto.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "debts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private BigDecimal valorTotal;
    private BigDecimal valorPago;
    private Double taxaJuros;
    private BigDecimal pagamentoMinimo;
    private String vencimento;
    private String status;      // ativa, quitada

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        if (valorPago == null) valorPago = BigDecimal.ZERO;
        if (status == null) status = "ativa";
    }
}