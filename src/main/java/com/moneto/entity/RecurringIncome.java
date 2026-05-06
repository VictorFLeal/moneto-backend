package com.moneto.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recurring_incomes")
public class RecurringIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;

    private BigDecimal valor;

    private String categoria;

    @Column(name = "regra_lancamento")
    private String regraLancamento;

    @Column(name = "dia_fixo")
    private Integer diaFixo;

    private Boolean ativo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @PrePersist
    public void prePersist() {
        if (ativo == null) ativo = true;
        if (regraLancamento == null || regraLancamento.isBlank()) regraLancamento = "QUINTO_DIA_UTIL";
        if (categoria == null || categoria.isBlank()) categoria = "Salário";
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getRegraLancamento() {
        return regraLancamento;
    }

    public void setRegraLancamento(String regraLancamento) {
        this.regraLancamento = regraLancamento;
    }

    public Integer getDiaFixo() {
        return diaFixo;
    }

    public void setDiaFixo(Integer diaFixo) {
        this.diaFixo = diaFixo;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}