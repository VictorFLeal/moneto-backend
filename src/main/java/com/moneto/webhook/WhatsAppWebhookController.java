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

                    if (value == null || value.getMessages() == null) continue;

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

    private String normalizePhone(String phone) {
        if (phone != null && phone.length() == 12) {
            return phone.substring(0, 4) + "9" + phone.substring(4);
        }

        return phone;
    }
}