package com.moneto.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/webhook/whatsapp")
public class WhatsappWebhookController {

    @Value("${WHATSAPP_VERIFY_TOKEN}")
    private String verifyToken;

    @Value("${WHATSAPP_ACCESS_TOKEN}")
    private String whatsappAccessToken;

    @Value("${WHATSAPP_PHONE_NUMBER_ID}")
    private String phoneNumberId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token inválido");
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            JsonNode messages = root
                    .path("entry")
                    .path(0)
                    .path("changes")
                    .path(0)
                    .path("value")
                    .path("messages");

            if (messages.isArray() && messages.size() > 0) {
                JsonNode message = messages.get(0);

                String from = message.path("from").asText();
                String type = message.path("type").asText();

                if ("text".equals(type)) {
                    String text = message.path("text").path("body").asText();

                    System.out.println("Mensagem recebida de " + from + ": " + text);

                    String resposta = interpretarMensagem(text);

                    enviarMensagem(from, resposta);
                }
            }

            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("EVENT_RECEIVED");
        }
    }

    private String interpretarMensagem(String texto) {
        String textoLower = texto.toLowerCase();

        if (textoLower.contains("gastei") || textoLower.contains("paguei")) {
            return "Despesa registrada no MONETO ✅\n\nMensagem recebida: " + texto;
        }

        if (textoLower.contains("recebi") || textoLower.contains("ganhei")) {
            return "Receita registrada no MONETO ✅\n\nMensagem recebida: " + texto;
        }

        return """
                Oi! Sou o MONETO 🤖

                Envie algo como:
                "gastei 50 com uber"
                ou
                "recebi 1200 de salário"
                """;
    }

    private void enviarMensagem(String to, String mensagem) {
        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(whatsappAccessToken);

        String payload = """
                {
                  "messaging_product": "whatsapp",
                  "to": "%s",
                  "type": "text",
                  "text": {
                    "body": "%s"
                  }
                }
                """.formatted(to, escaparJson(mensagem));

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(url, request, String.class);
    }

    private String escaparJson(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}