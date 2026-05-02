package com.moneto.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneto.entity.User;
import com.moneto.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public SettingsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getSettings(String email) {
        User user = getUser(email);

        Map<String, Object> settings = new LinkedHashMap<>();

        settings.put("nome", user.getNome());
        settings.put("sobrenome", user.getSobrenome());
        settings.put("email", user.getEmail());
        settings.put("telefone", user.getTelefone());
        settings.put("perfil", user.getPerfil());
        settings.put("plano", user.getPlano());
        settings.put("orcamentos", getOrcamentos(user));
        settings.put("notificacoes", getNotificacoes(user));

        return settings;
    }

    public Map<String, Object> updateSettings(String email, Map<String, Object> body) {
        User user = getUser(email);

        if (body.get("nome") != null) {
            user.setNome(String.valueOf(body.get("nome")));
        }

        if (body.get("sobrenome") != null) {
            user.setSobrenome(String.valueOf(body.get("sobrenome")));
        }

        if (body.get("telefone") != null) {
            user.setTelefone(String.valueOf(body.get("telefone")));
        }

        if (body.get("perfil") != null) {
            user.setPerfil(String.valueOf(body.get("perfil")));
        }

        if (body.get("plano") != null) {
            user.setPlano(String.valueOf(body.get("plano")));
        }

        if (body.get("orcamentos") instanceof Map<?, ?> orcamentos) {
            user.setOrcamentosJson(toJson(orcamentos));
        }

        if (body.get("notificacoes") instanceof Map<?, ?> notificacoes) {
            user.setNotificacoesJson(toJson(notificacoes));
        }

        String senhaAtual = body.get("senhaAtual") != null ? String.valueOf(body.get("senhaAtual")) : null;
        String novaSenha = body.get("novaSenha") != null ? String.valueOf(body.get("novaSenha")) : null;

        if (senhaAtual != null && novaSenha != null && !novaSenha.isBlank()) {
            if (!passwordEncoder.matches(senhaAtual, user.getPassword())) {
                throw new RuntimeException("Senha atual incorreta.");
            }

            if (novaSenha.length() < 8) {
                throw new RuntimeException("Nova senha deve ter no mínimo 8 caracteres.");
            }

            user.setPassword(passwordEncoder.encode(novaSenha));
        }

        userRepository.save(user);

        return getSettings(email);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private Map<String, Integer> getOrcamentos(User user) {
        if (user.getOrcamentosJson() == null || user.getOrcamentosJson().isBlank()) {
            return defaultOrcamentos();
        }

        try {
            return objectMapper.readValue(
                    user.getOrcamentosJson(),
                    new TypeReference<Map<String, Integer>>() {}
            );
        } catch (Exception e) {
            return defaultOrcamentos();
        }
    }

    private Map<String, Boolean> getNotificacoes(User user) {
        if (user.getNotificacoesJson() == null || user.getNotificacoesJson().isBlank()) {
            return defaultNotificacoes();
        }

        try {
            return objectMapper.readValue(
                    user.getNotificacoesJson(),
                    new TypeReference<Map<String, Boolean>>() {}
            );
        } catch (Exception e) {
            return defaultNotificacoes();
        }
    }

    private Map<String, Integer> defaultOrcamentos() {
        Map<String, Integer> orcamentos = new LinkedHashMap<>();
        orcamentos.put("Alimentação", 800);
        orcamentos.put("Transporte", 400);
        orcamentos.put("Moradia", 1200);
        orcamentos.put("Saúde", 300);
        orcamentos.put("Educação", 300);
        orcamentos.put("Lazer", 500);
        orcamentos.put("Outros", 300);
        return orcamentos;
    }

    private Map<String, Boolean> defaultNotificacoes() {
        Map<String, Boolean> notificacoes = new LinkedHashMap<>();
        notificacoes.put("app", true);
        notificacoes.put("whatsapp", true);
        notificacoes.put("alertasOrcamento", true);
        notificacoes.put("relatorioSemanal", false);
        return notificacoes;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar configurações.");
        }
    }
}