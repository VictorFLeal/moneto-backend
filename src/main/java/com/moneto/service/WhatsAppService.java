package com.moneto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

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

            Map<String, String> text = new HashMap<>();
            text.put("body", message);
            body.put("text", text);

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
        String message =
                "👋 Olá, *" + nome + "*! Bem-vindo ao *Moneto*!\n\n" +
                        "Agora podes gerir as tuas finanças diretamente aqui.\n\n" +
                        "*Como usar:*\n" +
                        "💸 Para despesa:\n" +
                        "\"gastei 50 no mercado\"\n\n" +
                        "💰 Para receita:\n" +
                        "\"recebi 3000 de salário\"\n\n" +
                        "📊 Para resumo:\n" +
                        "\"resumo do mês\"\n\n" +
                        "Testa agora 👆";

        sendMessage(to, message);
    }
}