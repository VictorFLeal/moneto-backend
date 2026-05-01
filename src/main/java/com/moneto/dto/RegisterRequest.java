package com.moneto.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String nome;
    @NotBlank private String sobrenome;
    @Email @NotBlank private String email;
    @NotBlank @Size(min = 8) private String password;
    private String telefone;
    private String perfil;
    private String plano;
}