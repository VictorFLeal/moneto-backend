package com.moneto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    @Value("${whatsapp.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String to, String message) {
        try {
            String url = apiUrl + "/" + phoneNumberId + "/messages";

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("recipient_type", "individual");
            body.put("to", to);
            body.put("type", "text");
            body.put("text", Map.of("body", message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
            log.info("WhatsApp message sent to {}", to);

        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp para {}: {}", to, e.getMessage());
        }
    }

    public void sendTemplateWelcome(String to, String nome) {
        String message = String.format("""
            👋 Olá, *%s*! Bem-vindo ao *Moneto*!
            
            Agora podes gerir as tuas finanças diretamente aqui.
            
            *Como usar:*
            💸 Para registar uma despesa:
            "gastei 50 no mercado"
            "paguei 120 na luz"
            
            💰 Para registar uma receita:
            "recebi 3000 de salário"
            "ganhei 500 de freela"
            
            📊 Para pedir um resumo:
            "resumo do mês"
            "qual meu saldo?"
            
            Experimenta agora! 👆
            """, nome);

        sendMessage(to, message);
    }
}