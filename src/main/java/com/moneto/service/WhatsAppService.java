package com.moneto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
            body.put("to", normalizePhone(to));

            // 🔥 ALTERAÇÃO AQUI (TEMPLATE)
            body.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", "hello_world");

            Map<String, String> language = new HashMap<>();
            language.put("code", "en_US");

            template.put("language", language);

            body.put("template", template);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            log.info("WhatsApp API status: {}", response.getStatusCode());
            log.info("WhatsApp API response: {}", response.getBody());
            log.info("WhatsApp message sent to {}", normalizePhone(to));

        } catch (HttpStatusCodeException e) {
            log.error("Erro HTTP ao enviar WhatsApp para {}", normalizePhone(to));
            log.error("Status: {}", e.getStatusCode());
            log.error("Response body: {}", e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp para {}: {}", normalizePhone(to), e.getMessage(), e);
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

        // ⚠️ aqui ainda chama sendMessage, mas no modo teste vai mandar template hello_world
        sendMessage(to, message);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        return phone.replaceAll("\\D", "");
    }
}