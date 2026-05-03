package com.moneto.webhook;

import com.moneto.dto.WhatsAppMessageDTO;
import com.moneto.service.TransactionParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    private final TransactionParserService transactionParserService;

    public WhatsAppWebhookController(TransactionParserService transactionParserService) {
        this.transactionParserService = transactionParserService;
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(403).body("Forbidden");
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppMessageDTO payload) {
        log.info("WhatsApp webhook received: {}", payload.getObject());

        try {
            if (!"whatsapp_business_account".equals(payload.getObject())) {
                return ResponseEntity.ok("OK");
            }

            List<WhatsAppMessageDTO.Entry> entries = payload.getEntry();

            if (entries == null || entries.isEmpty()) {
                return ResponseEntity.ok("OK");
            }

            for (WhatsAppMessageDTO.Entry entry : entries) {
                if (entry.getChanges() == null) continue;

                for (WhatsAppMessageDTO.Change change : entry.getChanges()) {
                    if (!"messages".equals(change.getField())) continue;

                    WhatsAppMessageDTO.Value value = change.getValue();

                    if (value == null) continue;

                    // ========================
                    // STATUS DA MENSAGEM ENVIADA
                    // sent / delivered / read / failed
                    // ========================
                    if (value.getStatuses() != null && !value.getStatuses().isEmpty()) {
                        for (WhatsAppMessageDTO.Status status : value.getStatuses()) {
                            logStatus(status);
                        }
                    }

                    // ========================
                    // MENSAGENS RECEBIDAS
                    // ========================
                    if (value.getMessages() == null || value.getMessages().isEmpty()) {
                        continue;
                    }

                    for (WhatsAppMessageDTO.Message msg : value.getMessages()) {
                        if (!"text".equals(msg.getType())) continue;
                        if (msg.getText() == null) continue;

                        String from = normalizePhone(msg.getFrom());
                        String text = msg.getText().getBody();
                        String name = "Utilizador";

                        if (value.getContacts() != null && !value.getContacts().isEmpty()) {
                            WhatsAppMessageDTO.Contact contact = value.getContacts().get(0);

                            if (contact.getProfile() != null && contact.getProfile().getName() != null) {
                                name = contact.getProfile().getName();
                            }
                        }

                        log.info("Message from {}: {}", from, text);

                        String finalFrom = from;
                        String finalName = name;

                        new Thread(() ->
                                transactionParserService.processWhatsAppMessage(finalFrom, text, finalName)
                        ).start();
                    }
                }
            }

        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("OK");
    }

    private void logStatus(WhatsAppMessageDTO.Status status) {
        if (status == null) {
            return;
        }

        String id = status.getId();
        String statusType = status.getStatus();
        String recipientId = status.getRecipientId();

        if ("sent".equals(statusType)) {
            log.info("✅ WHATSAPP SENT | wamid: {} | to: {}", id, recipientId);
            return;
        }

        if ("delivered".equals(statusType)) {
            log.info("📱 WHATSAPP DELIVERED | wamid: {} | to: {}", id, recipientId);
            return;
        }

        if ("read".equals(statusType)) {
            log.info("👁️ WHATSAPP READ | wamid: {} | to: {}", id, recipientId);
            return;
        }

        if ("failed".equals(statusType)) {
            String code = "?";
            String title = "?";
            String message = "?";
            Object errorData = null;

            if (status.getErrors() != null && !status.getErrors().isEmpty()) {
                WhatsAppMessageDTO.StatusError error = status.getErrors().get(0);

                if (error.getCode() != null) {
                    code = String.valueOf(error.getCode());
                }

                if (error.getTitle() != null) {
                    title = error.getTitle();
                }

                if (error.getMessage() != null) {
                    message = error.getMessage();
                }

                errorData = error.getErrorData();
            }

            log.error(
                    "❌ WHATSAPP FAILED | wamid: {} | to: {} | code: {} | title: {} | message: {} | errorData: {}",
                    id,
                    recipientId,
                    code,
                    title,
                    message,
                    errorData
            );
            return;
        }

        log.info("WHATSAPP STATUS | status: {} | wamid: {} | to: {}", statusType, id, recipientId);
    }

    private String normalizePhone(String phone) {
        if (phone != null && phone.length() == 12) {
            return phone.substring(0, 4) + "9" + phone.substring(4);
        }

        return phone;
    }
}