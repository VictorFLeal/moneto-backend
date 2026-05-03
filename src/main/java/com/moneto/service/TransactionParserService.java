package com.moneto.service;

import com.moneto.dto.ParsedTransactionDTO;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import com.moneto.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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

        String normalizedPhone = PhoneUtils.normalize(from);

        log.info("Mensagem recebida de {}: {}", normalizedPhone, messageText);

        Optional<User> userOpt = userRepository.findByTelefone(normalizedPhone);

        if (userOpt.isEmpty()) {
            whatsAppService.sendMessage(
                    from,
                    "❌ Não encontrei sua conta no MONETO.\n\n" +
                            "Verifique se esse número está cadastrado:\n" +
                            normalizedPhone
            );
            return;
        }

        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getTelefoneVerificado())) {
            whatsAppService.sendMessage(
                    from,
                    "🔐 Seu número ainda não foi verificado.\n\n" +
                            "Use o código enviado no cadastro para ativar o MONETO."
            );
            return;
        }

        if (!hasWhatsappAccess(user)) {
            whatsAppService.sendMessage(
                    from,
                    "🔒 Seu plano atual não inclui lançamentos via WhatsApp.\n\n" +
                            "Faça upgrade para usar essa função."
            );
            return;
        }

        if (isStart(user)) {
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
            LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            long whatsappUsado = transactionRepository
                    .countByUserIdAndOrigemAndDataBetween(user.getId(), "whatsapp", inicioMes, fimMes);

            if (whatsappUsado >= 10) {
                whatsAppService.sendMessage(
                        from,
                        "🔒 Você atingiu o limite de 10 mensagens via WhatsApp do plano Start neste mês.\n\n" +
                                "Faça upgrade para o MONETO Essencial e use WhatsApp sem esse limite."
                );
                return;
            }
        }

        ParsedTransactionDTO parsed = localParser.parseMessage(messageText);

        if (!Boolean.TRUE.equals(parsed.getParsed())) {
            log.warn("Parser local falhou, usando OpenAI...");
            parsed = openAIService.parseMessage(messageText);
        }

        if (!Boolean.TRUE.equals(parsed.getParsed())) {
            whatsAppService.sendMessage(
                    from,
                    "🤔 Não entendi sua mensagem.\n\n" +
                            (parsed.getErro() != null ? parsed.getErro() + "\n\n" : "") +
                            "Exemplos:\n" +
                            "• gastei 50 no mercado\n" +
                            "• paguei 30 no uber\n" +
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

        whatsAppService.sendMessage(
                from,
                "✅ Registrado no seu MONETO!\n\n" +
                        "Descrição: " + parsed.getDescricao() + "\n" +
                        "Categoria: " + parsed.getCategoria() + "\n" +
                        "Valor: " + sinal + formatMoney(parsed.getValor().doubleValue()) + "\n" +
                        "Saldo atual: " + formatMoney(saldo)
        );
    }

    private boolean hasWhatsappAccess(User user) {
        if (user.getPlano() == null) {
            return false;
        }

        String plano = user.getPlano().toLowerCase();

        return plano.equals("start")
                || plano.equals("essencial")
                || plano.equals("pro")
                || plano.equals("business");
    }

    private boolean isStart(User user) {
        return user.getPlano() == null || "start".equalsIgnoreCase(user.getPlano());
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

    private String formatMoney(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formatter.format(value);
    }
}