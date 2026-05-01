package com.moneto.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String nome;
    private String email;
    private String perfil;
    private String plano;
}