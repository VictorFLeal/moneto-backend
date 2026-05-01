package com.moneto.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ParsedTransactionDTO {
    private boolean parsed;
    private BigDecimal valor;
    private String tipo;
    private String categoria;
    private String descricao;
    private LocalDate data;
    private String mensagemOriginal;
    private String erro;
}