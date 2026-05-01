package com.moneto.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetDTO {
    private Long id;
    private String categoria;
    private BigDecimal limite;
    private String icone;
}