package com.moneto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BusinessEntryDTO {

    private Long id;
    private String descricao;
    private String tipo;
    private String categoria;
    private BigDecimal valor;
    private LocalDate data;
    private LocalDate vencimento;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}