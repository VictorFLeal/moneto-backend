package com.moneto.security;

import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

    // Remove caracteres perigosos para prevenir XSS e injeções
    public String sanitize(String input) {
        if (input == null) return null;
        return input
                .replaceAll("<[^>]*>", "")          // Remove tags HTML
                .replaceAll("javascript:", "")       // Remove JS
                .replaceAll("on\\w+\\s*=", "")      // Remove event handlers
                .replaceAll("[<>\"'`]", "")          // Remove chars especiais
                .trim();
    }

    public String sanitizeEmail(String email) {
        if (email == null) return null;
        return email.toLowerCase().trim().replaceAll("[^a-z0-9@._-]", "");
    }

    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    public boolean containsSqlInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("drop ") || lower.contains("delete ") ||
                lower.contains("insert ") || lower.contains("update ") ||
                lower.contains("select ") || lower.contains("union ") ||
                lower.contains("--") || lower.contains(";") ||
                lower.contains("exec(") || lower.contains("xp_");
    }
}