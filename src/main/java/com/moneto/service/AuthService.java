package com.moneto.service;

import com.moneto.dto.AuthRequest;
import com.moneto.dto.AuthResponse;
import com.moneto.dto.RegisterRequest;
import com.moneto.entity.User;
import com.moneto.repository.UserRepository;
import com.moneto.security.InputSanitizer;
import com.moneto.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final InputSanitizer sanitizer;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authManager,
            InputSanitizer sanitizer
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
        this.sanitizer = sanitizer;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = sanitizer.sanitizeEmail(req.getEmail());
        String nome = sanitizer.sanitize(req.getNome());
        String sobrenome = sanitizer.sanitize(req.getSobrenome());

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

        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new RuntimeException("Senha deve ter pelo menos 8 caracteres.");
        }

        User user = new User();
        user.setNome(nome);
        user.setSobrenome(sobrenome);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTelefone(req.getTelefone());
        user.setPerfil(req.getPerfil() != null ? req.getPerfil() : "individual");
        user.setPlano(req.getPlano() != null ? req.getPlano() : "start");

        userRepository.save(user);

        String token = jwtUtil.generateToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        return new AuthResponse(
                token,
                refreshToken,
                user.getNome(),
                email,
                user.getPerfil(),
                user.getPlano()
        );
    }

    public AuthResponse login(AuthRequest req) {
        String email = sanitizer.sanitizeEmail(req.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (user.isLocked()) {
            throw new RuntimeException("Conta bloqueada por " + LOCK_MINUTES + " minutos devido a múltiplas tentativas.");
        }

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.getPassword())
            );
        } catch (BadCredentialsException e) {
            Integer attempts = user.getFailedAttempts();

            if (attempts == null) {
                attempts = 0;
            }

            user.setFailedAttempts(attempts + 1);

            if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                userRepository.save(user);
                throw new RuntimeException("Conta bloqueada por " + LOCK_MINUTES + " minutos.");
            }

            userRepository.save(user);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        return new AuthResponse(
                token,
                refreshToken,
                user.getNome(),
                email,
                user.getPerfil(),
                user.getPlano()
        );
    }
}