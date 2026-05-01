package com.moneto.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DebtDTO {
    private Long id;
    private String nome;
    private BigDecimal valorTotal;
    private BigDecimal valorPago;
    private Double taxaJuros;
    private BigDecimal pagamentoMinimo;
    private String vencimento;
    private String status;
}