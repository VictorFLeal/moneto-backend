package com.moneto.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalDTO {
    private Long id;
    private String titulo;
    private String icone;
    private BigDecimal valorMeta;
    private BigDecimal valorAtual;
    private LocalDate prazo;
    private String status;
}