package com.moneto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionDTO {
    private Long id;
    @NotBlank private String descricao;
    @NotNull  private BigDecimal valor;
    @NotBlank private String tipo;
    @NotBlank private String categoria;
    private LocalDate data;
    private String origem;
}