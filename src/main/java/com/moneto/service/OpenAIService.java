package com.moneto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneto.dto.ParsedTransactionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
        Você é um assistente financeiro do aplicativo Moneto.
        Sua função é extrair informações financeiras de mensagens em português brasileiro.

        Quando o utilizador enviar uma mensagem, extrai:
        - valor: número decimal (ex: 50.00)
        - tipo: "RECEITA" ou "DESPESA"
        - categoria: Alimentação, Transporte, Moradia, Lazer, Saúde, Educação, Vestuário, Trabalho, Tecnologia, Salário, Freelance, Investimento, Outros
        - descricao: descrição curta e clara da transação

        Responde APENAS com JSON válido, sem markdown, sem explicações.

        Exemplo válido:
        {
          "parsed": true,
          "valor": 50.00,
          "tipo": "DESPESA",
          "categoria": "Alimentação",
          "descricao": "iFood"
        }

        Se não conseguires extrair informação financeira:
        {
          "parsed": false,
          "erro": "Não entendi a transação."
        }
        """;

    public ParsedTransactionDTO parseMessage(String userMessage) {
        ParsedTransactionDTO result = new ParsedTransactionDTO();
        result.setMensagemOriginal(userMessage);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 200);
            body.put("temperature", 0.1);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    OPENAI_URL,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            log.info("OpenAI response for '{}': {}", userMessage, content);

            JsonNode parsed = objectMapper.readTree(content.trim());

            result.setParsed(parsed.path("parsed").asBoolean(false));

            if (Boolean.TRUE.equals(result.getParsed())) {
                result.setValor(new BigDecimal(parsed.path("valor").asText("0")));
                result.setTipo(parsed.path("tipo").asText("DESPESA"));
                result.setCategoria(parsed.path("categoria").asText("Outros"));
                result.setDescricao(parsed.path("descricao").asText(userMessage));
            } else {
                result.setErro(parsed.path("erro").asText("Não entendi a mensagem."));
            }

        } catch (Exception e) {
            log.error("Erro ao chamar OpenAI: {}", e.getMessage());

            result.setParsed(false);
            result.setErro("Erro ao processar a mensagem. Tenta novamente.");
        }

        return result;
    }
}