package com.moneto.service;

import com.moneto.dto.AuthRequest;
import com.moneto.dto.AuthResponse;
import com.moneto.dto.RegisterRequest;
import com.moneto.dto.VerifyPhoneRequest;
import com.moneto.entity.User;
import com.moneto.repository.UserRepository;
import com.moneto.security.InputSanitizer;
import com.moneto.security.JwtUtil;
import com.moneto.util.PhoneUtils;
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
    private final WhatsAppService whatsAppService;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authManager,
            InputSanitizer sanitizer,
            WhatsAppService whatsAppService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
        this.sanitizer = sanitizer;
        this.whatsAppService = whatsAppService;
    }

    // ========================
    // REGISTER
    // ========================
    public AuthResponse register(RegisterRequest req) {

        String email = sanitizer.sanitizeEmail(req.getEmail());
        String nome = sanitizer.sanitize(req.getNome());
        String sobrenome = sanitizer.sanitize(req.getSobrenome());
        String telefone = PhoneUtils.normalize(req.getTelefone());

        if (!sanitizer.isValidEmail(email)) {
            throw new RuntimeException("E-mail inválido.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("E-mail já cadastrado.");
        }

        if (userRepository.existsByTelefone(telefone)) {
            throw new RuntimeException("Telefone já cadastrado.");
        }

        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new RuntimeException("Senha deve ter pelo menos 8 caracteres.");
        }

        String codigo = gerarCodigo();

        User user = new User();
        user.setNome(nome);
        user.setSobrenome(sobrenome);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setTelefone(telefone);
        user.setPerfil(req.getPerfil() != null ? req.getPerfil() : "individual");
        user.setPlano(req.getPlano() != null ? req.getPlano() : "start");

        user.setTelefoneVerificado(false);
        user.setCodigoVerificacao(codigo);
        user.setCodigoExpiraEm(LocalDateTime.now().plusMinutes(10));
        user.setCodigoUltimoEnvio(LocalDateTime.now());

        userRepository.save(user);

        whatsAppService.sendMessage(
                telefone,
                "🔐 Código de verificação MONETO:\n\n" +
                        codigo +
                        "\n\nVálido por 10 minutos."
        );

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

    // ========================
    // LOGIN
    // ========================
    public AuthResponse login(AuthRequest req) {

        String email = sanitizer.sanitizeEmail(req.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (user.isLocked()) {
            throw new RuntimeException("Conta bloqueada temporariamente.");
        }

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.getPassword())
            );
        } catch (BadCredentialsException e) {

            Integer attempts = user.getFailedAttempts();
            if (attempts == null) attempts = 0;

            user.setFailedAttempts(attempts + 1);

            if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
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

    // ========================
    // VERIFY PHONE
    // ========================
    public String verifyPhone(VerifyPhoneRequest req) {

        String telefone = PhoneUtils.normalize(req.getTelefone());

        User user = userRepository.findByTelefone(telefone)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getCodigoVerificacao() == null) {
            throw new RuntimeException("Nenhum código gerado.");
        }

        if (user.getCodigoExpiraEm() == null) {
            throw new RuntimeException("Código sem data de expiração.");
        }

        if (user.getCodigoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Código expirado.");
        }

        if (!user.getCodigoVerificacao().equals(req.getCodigo())) {
            throw new RuntimeException("Código inválido.");
        }

        user.setTelefoneVerificado(true);
        user.setCodigoVerificacao(null);
        user.setCodigoExpiraEm(null);

        userRepository.save(user);

        return "Telefone verificado com sucesso ✅";
    }

    // ========================
    // 🔥 RESEND CODE (NOVO)
    // ========================
    public String resendCode(String telefone) {

        String normalized = PhoneUtils.normalize(telefone);

        User user = userRepository.findByTelefone(normalized)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (Boolean.TRUE.equals(user.getTelefoneVerificado())) {
            throw new RuntimeException("Telefone já verificado.");
        }

        if (user.getCodigoUltimoEnvio() != null &&
                user.getCodigoUltimoEnvio()
                        .plusSeconds(RESEND_COOLDOWN_SECONDS)
                        .isAfter(LocalDateTime.now())) {

            throw new RuntimeException("Aguarde 30 segundos antes de solicitar outro código.");
        }

        String codigo = gerarCodigo();

        user.setCodigoVerificacao(codigo);
        user.setCodigoExpiraEm(LocalDateTime.now().plusMinutes(10));
        user.setCodigoUltimoEnvio(LocalDateTime.now());

        userRepository.save(user);

        whatsAppService.sendMessage(
                normalized,
                "🔐 Novo código MONETO:\n\n" +
                        codigo +
                        "\n\nVálido por 10 minutos."
        );

        return "Código reenviado com sucesso";
    }

    // ========================
    // GERAR CÓDIGO
    // ========================
    private String gerarCodigo() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}