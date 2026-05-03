package com.moneto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nome;

    @NotBlank
    @Column(nullable = false)
    private String sobrenome;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String perfil;

    @Column(nullable = false)
    private String plano;

    @Column(name = "orcamentos_json", columnDefinition = "TEXT")
    private String orcamentosJson;

    @Column(name = "notificacoes_json", columnDefinition = "TEXT")
    private String notificacoesJson;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "telefone_verificado")
    private Boolean telefoneVerificado = false;

    @Column(name = "codigo_verificacao")
    private String codigoVerificacao;

    @Column(name = "codigo_ultimo_envio")
    private LocalDateTime codigoUltimoEnvio;

    @Column(name = "codigo_expira_em")
    private LocalDateTime codigoExpiraEm;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (plano == null) plano = "start";
        if (perfil == null) perfil = "individual";
        if (failedAttempts == null) failedAttempts = 0;
        if (telefoneVerificado == null) telefoneVerificado = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }

    public String getOrcamentosJson() {
        return orcamentosJson;
    }

    public void setOrcamentosJson(String orcamentosJson) {
        this.orcamentosJson = orcamentosJson;
    }

    public String getNotificacoesJson() {
        return notificacoesJson;
    }

    public void setNotificacoesJson(String notificacoesJson) {
        this.notificacoesJson = notificacoesJson;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ========================
    // TELEFONE VERIFICADO
    // ========================
    public Boolean getTelefoneVerificado() {
        return telefoneVerificado;
    }

    public void setTelefoneVerificado(Boolean telefoneVerificado) {
        this.telefoneVerificado = telefoneVerificado;
    }

    // ========================
    // CÓDIGO DE VERIFICAÇÃO
    // ========================
    public String getCodigoVerificacao() {
        return codigoVerificacao;
    }

    public void setCodigoVerificacao(String codigoVerificacao) {
        this.codigoVerificacao = codigoVerificacao;
    }

    // ========================
    // ÚLTIMO ENVIO DO CÓDIGO
    // ========================
    public LocalDateTime getCodigoUltimoEnvio() {
        return codigoUltimoEnvio;
    }

    public void setCodigoUltimoEnvio(LocalDateTime codigoUltimoEnvio) {
        this.codigoUltimoEnvio = codigoUltimoEnvio;
    }

    // ========================
    // EXPIRAÇÃO DO CÓDIGO
    // ========================
    public LocalDateTime getCodigoExpiraEm() {
        return codigoExpiraEm;
    }

    public void setCodigoExpiraEm(LocalDateTime codigoExpiraEm) {
        this.codigoExpiraEm = codigoExpiraEm;
    }
}