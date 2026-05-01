package com.moneto.service;

import com.moneto.dto.*;
import com.moneto.entity.User;
import com.moneto.repository.UserRepository;
import com.moneto.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtUtil            jwtUtil;
    private final AuthenticationManager authManager;
    private final InputSanitizer     sanitizer;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    public AuthResponse register(RegisterRequest req) {

        // Sanitização dos inputs
        String email = sanitizer.sanitizeEmail(req.getEmail());
        String nome  = sanitizer.sanitize(req.getNome());

        // Validações de segurança
        if (!sanitizer.isValidEmail(email)) {
            throw new RuntimeException("E-mail inválido.");
        }
        if (sanitizer.containsSqlInjection(req.getNome()) ||
                sanitizer.containsSqlInjection(req.getSobrenome())) {
            throw new RuntimeException("Dados inválidos.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("E-mail já cadastrado.");
        }
        if (req.getPassword().length() < 8) {
            throw new RuntimeException("Senha deve ter pelo menos 8 caracteres.");
        }

        User user = User.builder()
                .nome(nome)
                .sobrenome(sanitizer.sanitize(req.getSobrenome()))
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .telefone(req.getTelefone())
                .perfil(req.getPerfil() != null ? req.getPerfil() : "individual")
                .plano(req.getPlano() != null ? req.getPlano() : "start")
                .build();

        userRepository.save(user);

        String token        = jwtUtil.generateToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        return new AuthResponse(token, refreshToken, user.getNome(), email, user.getPerfil(), user.getPlano());
    }

    public AuthResponse login(AuthRequest req) {

        String email = sanitizer.sanitizeEmail(req.getEmail());

        // Verifica se o utilizador existe
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        // Verifica se a conta está bloqueada
        if (user.isLocked()) {
            throw new RuntimeException("Conta bloqueada por " + LOCK_MINUTES + " minutos devido a múltiplas tentativas.");
        }

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Incrementa tentativas falhadas
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                userRepository.save(user);
                throw new RuntimeException("Conta bloqueada por " + LOCK_MINUTES + " minutos.");
            }
            userRepository.save(user);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        // Login bem sucedido — reset tentativas
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token        = jwtUtil.generateToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        return new AuthResponse(token, refreshToken, user.getNome(), email, user.getPerfil(), user.getPlano());
    }
}