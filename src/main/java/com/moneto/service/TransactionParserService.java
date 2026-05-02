package com.moneto.service;

import com.moneto.dto.ParsedTransactionDTO;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionParserService {

    private static final Logger log = LoggerFactory.getLogger(TransactionParserService.class);

    private final OpenAIService openAIService;
    private final LocalTransactionParserService localParser;
    private final WhatsAppService whatsAppService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionParserService(
            OpenAIService openAIService,
            LocalTransactionParserService localParser,
            WhatsAppService whatsAppService,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.openAIService = openAIService;
        this.localParser = localParser;
        this.whatsAppService = whatsAppService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public void processWhatsAppMessage(String from, String messageText, String senderName) {

        log.info("Mensagem recebida de {}: {}", from, messageText);

        Optional<User> userOpt = userRepository.findByTelefone(from);

        if (userOpt.isEmpty()) {
            whatsAppService.sendMessage(from, "❌ Número não cadastrado no Moneto.");
            return;
        }

        User user = userOpt.get();

        ParsedTransactionDTO parsed = localParser.parseMessage(messageText);

        if (!Boolean.TRUE.equals(parsed.getParsed())) {
            log.warn("Parser local falhou, usando OpenAI...");
            parsed = openAIService.parseMessage(messageText);
        }

        if (!Boolean.TRUE.equals(parsed.getParsed())) {
            whatsAppService.sendMessage(from,
                    "🤔 Não entendi...\n\n" +
                            (parsed.getErro() != null ? parsed.getErro() : "") +
                            "\n\nExemplos:\n" +
                            "• gastei 50 no mercado\n" +
                            "• recebi 3000 de salário"
            );
            return;
        }

        salvarTransacao(user, from, parsed);
    }

    private void salvarTransacao(User user, String from, ParsedTransactionDTO parsed) {

        Transaction tx = new Transaction();
        tx.setDescricao(parsed.getDescricao());
        tx.setValor(parsed.getValor());
        tx.setTipo(parsed.getTipo());
        tx.setCategoria(parsed.getCategoria());
        tx.setData(parsed.getData() != null ? parsed.getData() : LocalDate.now());
        tx.setOrigem("whatsapp");
        tx.setUser(user);

        transactionRepository.save(tx);

        double saldo = calcularSaldo(user);

        String sinal = "RECEITA".equals(parsed.getTipo()) ? "+" : "-";

        whatsAppService.sendMessage(from,
                "✅ Registrado!\n\n" +
                        parsed.getDescricao() + "\n" +
                        sinal + "R$ " + parsed.getValor() + "\n" +
                        "Saldo: R$ " + String.format("%.2f", saldo)
        );
    }

    private double calcularSaldo(User user) {

        List<Transaction> list = transactionRepository.findByUserId(user.getId());

        double receitas = list.stream()
                .filter(t -> "RECEITA".equals(t.getTipo()))
                .mapToDouble(t -> t.getValor().doubleValue())
                .sum();

        double despesas = list.stream()
                .filter(t -> "DESPESA".equals(t.getTipo()))
                .mapToDouble(t -> t.getValor().doubleValue())
                .sum();

        return receitas - despesas;
    }
}