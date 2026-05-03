package com.moneto.controller;

import com.moneto.dto.AuthRequest;
import com.moneto.dto.AuthResponse;
import com.moneto.dto.RegisterRequest;
import com.moneto.dto.VerifyPhoneRequest;
import com.moneto.security.JwtUtil;
import com.moneto.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    // ========================
    // REGISTRO
    // ========================
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            AuthResponse response = authService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // LOGIN
    // ========================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {
        try {
            AuthResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // VERIFICAR TELEFONE
    // ========================
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody VerifyPhoneRequest req) {
        try {
            String response = authService.verifyPhone(req);
            return ResponseEntity.ok(java.util.Map.of("message", response));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // REENVIAR CÓDIGO
    // ========================
    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody java.util.Map<String, String> body) {
        try {
            String telefone = body.get("telefone");
            String response = authService.resendCode(telefone);

            return ResponseEntity.ok(java.util.Map.of("message", response));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // REFRESH TOKEN
    // ========================
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("error", "Token não fornecido"));
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.isValid(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("error", "Token expirado ou inválido"));
            }

            String email = jwtUtil.extractEmail(token);
            String newToken = jwtUtil.generateToken(email);

            return ResponseEntity.ok(java.util.Map.of("token", newToken));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Erro ao gerar novo token"));
        }
    }
}