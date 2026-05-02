package com.moneto.dto;

public class AuthResponse {

    private String token;
    private String refreshToken;
    private String nome;
    private String email;
    private String perfil;
    private String plano;

    public AuthResponse() {
    }

    public AuthResponse(String token, String refreshToken, String nome, String email, String perfil, String plano) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.plano = plano;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}