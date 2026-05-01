package com.moneto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneto.dto.ParsedTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

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
        - tipo: "RECEITA" (ganho, recebeu, entrada) ou "DESPESA" (gastou, pagou, saída)
        - categoria: uma das seguintes: Alimentação, Transporte, Moradia, Lazer, Saúde, Educação, Vestuário, Trabalho, Tecnologia, Salário, Freelance, Investimento, Outros
        - descricao: descrição curta e clara da transação
        
        Responde APENAS com JSON válido, sem markdown, sem explicações:
        {
          "parsed": true,
          "valor": 50.00,
          "tipo": "DESPESA",
          "categoria": "Alimentação",
          "descricao": "iFood"
        }
        
        Se não conseguires extrair informação financeira, responde:
        {
          "parsed": false,
          "erro": "Não entendi a transação. Tenta: 'gastei 50 no mercado' ou 'recebi 1000 de salário'"
        }
        
        Exemplos:
        - "gastei 50 reais com ifood" → DESPESA, 50.00, Alimentação
        - "paguei 120 na luz" → DESPESA, 120.00, Moradia
        - "recebi 3000 de salário" → RECEITA, 3000.00, Salário
        - "ganhei 500 de freela" → RECEITA, 500.00, Freelance
        - "uber custou 28 reais" → DESPESA, 28.00, Transporte
        - "comprei remédio 45 reais" → DESPESA, 45.00, Saúde
        """;

    public ParsedTransactionDTO parseMessage(String userMessage) {
        ParsedTransactionDTO result = new ParsedTransactionDTO();
        result.setMensagemOriginal(userMessage);

        try {
            // Monta o request para a OpenAI
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 200);
            body.put("temperature", 0.1); // Baixa temperatura para respostas consistentes

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    OPENAI_URL, entity, String.class
            );

            // Extrai o conteúdo da resposta
            JsonNode root    = objectMapper.readTree(response.getBody());
            String content   = root.path("choices").get(0).path("message").path("content").asText();

            log.info("OpenAI response for '{}': {}", userMessage, content);

            // Parse do JSON retornado pela IA
            JsonNode parsed = objectMapper.readTree(content.trim());

            result.setParsed(parsed.path("parsed").asBoolean(false));

            if (result.isParsed()) {
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