package com.moneto.dto;

public class VerifyPhoneRequest {

    private String telefone;
    private String codigo;

    public String getTelefone() {
        return telefone;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}