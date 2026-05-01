package com.moneto.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String icone;
    private BigDecimal valorMeta;
    private BigDecimal valorAtual;
    private LocalDate prazo;
    private String status;      // ativa, concluida

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    public void prePersist() {
        if (valorAtual == null) valorAtual = BigDecimal.ZERO;
        if (status == null) status = "ativa";
    }
}