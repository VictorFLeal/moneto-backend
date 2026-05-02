package com.moneto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParsedTransactionDTO {

    private String mensagemOriginal;
    private Boolean parsed;
    private String erro;
    private BigDecimal valor;
    private String tipo;
    private String categoria;
    private String descricao;
    private LocalDate data;

    public String getMensagemOriginal() {
        return mensagemOriginal;
    }

    public void setMensagemOriginal(String mensagemOriginal) {
        this.mensagemOriginal = mensagemOriginal;
    }

    public Boolean getParsed() {
        return parsed;
    }

    public void setParsed(Boolean parsed) {
        this.parsed = parsed;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }
}