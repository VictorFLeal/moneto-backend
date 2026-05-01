package com.moneto.service;

import com.moneto.dto.ParsedTransactionDTO;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionParserService {

    private final OpenAIService openAIService;
    private final LocalTransactionParserService localTransactionParserService;
    private final WhatsAppService whatsAppService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private final Map<String, ConversationContext> contexts =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static class ConversationContext {
        String pendingDescricao;
        BigDecimal pendingValor;
        String pendingTipo;
        LocalDate pendingData;
        boolean waitingForCategory = false;
        boolean waitingForValue = false;
        LocalDateTime lastActivity = LocalDateTime.now();

        boolean isExpired() {
            return lastActivity.plusMinutes(10).isBefore(LocalDateTime.now());
        }

        void reset() {
            pendingDescricao = null;
            pendingValor = null;
            pendingTipo = null;
            pendingData = null;
            waitingForCategory = false;
            waitingForValue = false;
        }

        void touch() {
            lastActivity = LocalDateTime.now();
        }
    }

    public void processWhatsAppMessage(String from, String messageText, String senderName) {
        log.info("Processing message from {}: {}", from, messageText);

        String phone = normalizePhone(from);

        Optional<User> userOpt = userRepository.findByTelefone(phone)
                .or(() -> userRepository.findByTelefone(from));

        if (userOpt.isEmpty()) {
            whatsAppService.sendMessage(from,
                    "❌ Número não encontrado no Moneto.\n\n" +
                            "Regista-te em: https://moneto.app/register\n" +
                            "E adiciona este número no teu perfil!"
            );
            return;
        }

        User user = userOpt.get();
        String msg = messageText.trim();
        String msgLower = msg.toLowerCase();

        ConversationContext ctx = contexts.computeIfAbsent(from, k -> new ConversationContext());

        if (ctx.isExpired()) {
            ctx.reset();
        }

        ctx.touch();

        if (ctx.waitingForCategory) {
            handleCategoryResponse(user, from, msg, ctx);
            return;
        }

        if (ctx.waitingForValue) {
            handleValueResponse(user, from, msg, ctx);
            return;
        }

        if (isAjudaCommand(msgLower)) {
            sendAjuda(from, user.getNome());
            return;
        }

        if (isResumoMesCommand(msgLower)) {
            sendResumoMes(user, from);
            return;
        }

        if (isResumoHojeCommand(msgLower)) {
            sendResumoHoje(user, from);
            return;
        }

        if (isSaldoCommand(msgLower)) {
            sendSaldo(user, from);
            return;
        }

        if (isUltimosCommand(msgLower)) {
            sendUltimosGastos(user, from);
            return;
        }

        if (isOrcamentoCommand(msgLower)) {
            sendOrcamento(user, from, msgLower);
            return;
        }

        if (isMetaCommand(msgLower)) {
            sendMeta(user, from);
            return;
        }

        if (isCorrecaoCommand(msgLower)) {
            handleCorrecao(user, from, msg);
            return;
        }

        if (isApagaCommand(msgLower)) {
            handleApaga(user, from);
            return;
        }

        if (isCategoriaCommand(msgLower)) {
            sendCategoriaMaisGasta(user, from);
            return;
        }

        if (isPiorDiaCommand(msgLower)) {
            sendPiorDia(user, from);
            return;
        }

        if (isInsightCommand(msgLower)) {
            sendInsight(user, from);
            return;
        }

        if (isGastoCategoria(msgLower)) {
            sendGastoPorCategoria(user, from, msgLower);
            return;
        }

        ParsedTransactionDTO parsed = localTransactionParserService.parseMessage(msg);

        if (!parsed.isParsed()) {
            log.warn("Parser local não entendeu. Tentando OpenAI...");
            parsed = openAIService.parseMessage(msg);
        }

        if (!parsed.isParsed()) {
            whatsAppService.sendMessage(from,
                    "🤔 Não entendi bem...\n\n" +
                            (parsed.getErro() != null ? parsed.getErro() : "") +
                            "\n\n💡 Exemplos:\n" +
                            "• \"gastei 50 no mercado\"\n" +
                            "• \"recebi 3000 de salário\"\n" +
                            "• \"paguei 120 na conta de luz\"\n\n" +
                            "Digita *ajuda* para ver todos os comandos."
            );
            return;
        }

        if (parsed.getValor() == null || parsed.getValor().compareTo(BigDecimal.ZERO) == 0) {
            ctx.pendingDescricao = parsed.getDescricao();
            ctx.pendingTipo = parsed.getTipo();
            ctx.pendingData = parsed.getData();
            ctx.waitingForValue = true;

            whatsAppService.sendMessage(from,
                    "💰 Qual foi o valor?\n\nResponde só com o número, ex: *50*"
            );
            return;
        }

        saveAndConfirm(user, from, parsed);
        ctx.reset();
    }

    private void handleCategoryResponse(User user, String from, String resp, ConversationContext ctx) {
        String categoria = mapCategoria(resp.toLowerCase());

        Transaction tx = Transaction.builder()
                .descricao(ctx.pendingDescricao)
                .valor(ctx.pendingValor)
                .tipo(ctx.pendingTipo != null ? ctx.pendingTipo : "DESPESA")
                .categoria(categoria)
                .data(LocalDate.now())
                .origem("whatsapp")
                .user(user)
                .build();

        transactionRepository.save(tx);
        ctx.reset();

        double saldo = calcSaldo(user);
        String sinal = "DESPESA".equals(tx.getTipo()) ? "−" : "+";

        whatsAppService.sendMessage(from,
                "✅ *Registado!*\n\n" +
                        "📝 " + tx.getDescricao() + "\n" +
                        "💵 " + sinal + "R$ " + tx.getValor().toPlainString() + "\n" +
                        "🏷️ " + categoria + "\n\n" +
                        "💳 Saldo atual: *R$ " + String.format("%.2f", saldo) + "*"
        );
    }

    private void handleValueResponse(User user, String from, String resp, ConversationContext ctx) {
        try {
            BigDecimal valor = new BigDecimal(resp.replace(",", ".").replaceAll("[^0-9.]", ""));

            ParsedTransactionDTO dto = new ParsedTransactionDTO();
            dto.setParsed(true);
            dto.setValor(valor);
            dto.setTipo(ctx.pendingTipo != null ? ctx.pendingTipo : "DESPESA");
            dto.setCategoria("Outros");
            dto.setDescricao(ctx.pendingDescricao != null ? ctx.pendingDescricao : "Gasto");
            dto.setData(ctx.pendingData != null ? ctx.pendingData : LocalDate.now());

            ctx.reset();
            saveAndConfirm(user, from, dto);

        } catch (Exception e) {
            whatsAppService.sendMessage(from,
                    "❌ Não entendi o valor. Responde só com o número, ex: *50*"
            );
        }
    }

    private void saveAndConfirm(User user, String from, ParsedTransactionDTO parsed) {
        Transaction tx = Transaction.builder()
                .descricao(parsed.getDescricao())
                .valor(parsed.getValor())
                .tipo(parsed.getTipo())
                .categoria(parsed.getCategoria())
                .data(parsed.getData() != null ? parsed.getData() : LocalDate.now())
                .origem("whatsapp")
                .user(user)
                .build();

        transactionRepository.save(tx);

        double saldo = calcSaldo(user);
        String emoji = "RECEITA".equals(parsed.getTipo()) ? "💰" : "💸";
        String sinal = "RECEITA".equals(parsed.getTipo()) ? "+" : "−";

        whatsAppService.sendMessage(from,
                emoji + " *Transação registada!*\n\n" +
                        "📝 " + parsed.getDescricao() + "\n" +
                        "💵 " + sinal + "R$ " + parsed.getValor().toPlainString() + "\n" +
                        "🏷️ " + parsed.getCategoria() + "\n" +
                        "📅 " + (parsed.getData() != null ? parsed.getData() : LocalDate.now()) + "\n\n" +
                        "💳 Saldo atual: *R$ " + String.format("%.2f", saldo) + "*"
        );

        checkBudgetAlert(user, from, parsed.getCategoria());
    }

    private void sendResumoMes(User user, String from) {
        double receitas = getSumByTipo(user, "RECEITA");
        double despesas = getSumByTipo(user, "DESPESA");
        double saldo = receitas - despesas;

        List<Transaction> txs = transactionRepository.findByUserIdOrderByDataDesc(user.getId());
        Map<String, Double> porCategoria = new java.util.LinkedHashMap<>();

        txs.stream()
                .filter(t -> "DESPESA".equals(t.getTipo()))
                .forEach(t -> porCategoria.merge(t.getCategoria(), t.getValor().doubleValue(), Double::sum));

        StringBuilder sb = new StringBuilder();
        sb.append("📊 *Resumo do mês*\n\n");
        sb.append("💰 Receitas: R$ ").append(String.format("%.2f", receitas)).append("\n");
        sb.append("💸 Despesas: R$ ").append(String.format("%.2f", despesas)).append("\n");
        sb.append("💳 Saldo: *R$ ").append(String.format("%.2f", saldo)).append("*\n\n");
        sb.append("📈 *Top categorias:*\n");

        porCategoria.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append("• ").append(e.getKey())
                        .append(": R$ ").append(String.format("%.2f", e.getValue())).append("\n"));

        sb.append("\n").append(saldo >= 0 ? "✅ No positivo!" : "⚠️ Saldo negativo. Cuidado!");

        whatsAppService.sendMessage(from, sb.toString());
    }

    private void sendResumoHoje(User user, String from) {
        List<Transaction> hoje = transactionRepository
                .findByUserIdAndDataBetweenOrderByDataDesc(user.getId(), LocalDate.now(), LocalDate.now());

        if (hoje.isEmpty()) {
            whatsAppService.sendMessage(from, "📅 Nenhum gasto registado hoje ainda!");
            return;
        }

        double total = hoje.stream()
                .filter(t -> "DESPESA".equals(t.getTipo()))
                .mapToDouble(t -> t.getValor().doubleValue())
                .sum();

        StringBuilder sb = new StringBuilder("📅 *Gastos de hoje:*\n\n");

        hoje.forEach(t -> sb.append("• ").append(t.getDescricao())
                .append(" — R$ ").append(t.getValor().toPlainString()).append("\n"));

        sb.append("\n💸 Total: *R$ ").append(String.format("%.2f", total)).append("*");

        whatsAppService.sendMessage(from, sb.toString());
    }

    private void sendSaldo(User user, String from) {
        double saldo = calcSaldo(user);

        whatsAppService.sendMessage(from,
                "💳 *Teu saldo atual:*\n\n*R$ " + String.format("%.2f", saldo) + "*\n\n" +
                        (saldo >= 0 ? "✅ No positivo!" : "⚠️ Saldo negativo. Cuidado!")
        );
    }

    private void sendUltimosGastos(User user, String from) {
        List<Transaction> txs = transactionRepository
                .findByUserIdOrderByDataDesc(user.getId())
                .stream()
                .limit(5)
                .toList();

        if (txs.isEmpty()) {
            whatsAppService.sendMessage(from, "📋 Nenhuma transação registada ainda.");
            return;
        }

        StringBuilder sb = new StringBuilder("📋 *Últimas transações:*\n\n");

        txs.forEach(t -> {
            String sinal = "DESPESA".equals(t.getTipo()) ? "−" : "+";
            sb.append("• ").append(t.getDescricao())
                    .append(" (").append(t.getCategoria()).append(")")
                    .append(" — ").append(sinal).append("R$ ")
                    .append(t.getValor().toPlainString()).append("\n");
        });

        whatsAppService.sendMessage(from, sb.toString());
    }

    private void sendOrcamento(User user, String from, String msg) {
        whatsAppService.sendMessage(from,
                "🎯 *Orçamento do mês:*\n\n" +
                        "Acede ao dashboard para ver os teus orçamentos detalhados!\n\n" +
                        "👉 https://moneto.app/dashboard"
        );
    }

    private void sendMeta(User user, String from) {
        whatsAppService.sendMessage(from,
                "🎯 *As tuas metas:*\n\n" +
                        "Acede ao dashboard para ver o progresso!\n\n" +
                        "👉 https://moneto.app/dashboard/goals"
        );
    }

    private void handleCorrecao(User user, String from, String msg) {
        List<Transaction> txs = transactionRepository.findByUserIdOrderByDataDesc(user.getId());

        if (txs.isEmpty()) {
            whatsAppService.sendMessage(from, "❌ Nenhuma transação para corrigir.");
            return;
        }

        Transaction last = txs.get(0);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\d+[\\.,]?\\d*")
                .matcher(msg);

        if (m.find()) {
            BigDecimal novoValor = new BigDecimal(m.group().replace(",", "."));
            last.setValor(novoValor);
            transactionRepository.save(last);

            whatsAppService.sendMessage(from,
                    "✅ *Corrigido!*\n\n📝 " + last.getDescricao() + "\n💵 R$ " + novoValor.toPlainString()
            );
        } else {
            whatsAppService.sendMessage(from,
                    "✏️ Último gasto: *" + last.getDescricao() + "* — R$ " + last.getValor().toPlainString() + "\n\n" +
                            "Responde: \"corrige para 30 reais\""
            );
        }
    }

    private void handleApaga(User user, String from) {
        List<Transaction> txs = transactionRepository.findByUserIdOrderByDataDesc(user.getId());

        if (txs.isEmpty()) {
            whatsAppService.sendMessage(from, "❌ Nenhuma transação para apagar.");
            return;
        }

        Transaction last = txs.get(0);
        transactionRepository.delete(last);

        whatsAppService.sendMessage(from,
                "🗑️ *Apagado!*\n\n" + last.getDescricao() + " — R$ " + last.getValor().toPlainString()
        );
    }

    private void sendCategoriaMaisGasta(User user, String from) {
        List<Transaction> txs = transactionRepository.findByUserIdOrderByDataDesc(user.getId());
        Map<String, Double> por = new java.util.HashMap<>();

        txs.stream()
                .filter(t -> "DESPESA".equals(t.getTipo()))
                .forEach(t -> por.merge(t.getCategoria(), t.getValor().doubleValue(), Double::sum));

        if (por.isEmpty()) {
            whatsAppService.sendMessage(from, "📊 Nenhuma despesa registada ainda.");
            return;
        }

        String top = por.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (R$ " + String.format("%.2f", e.getValue()) + ")")
                .orElse("Nenhuma");

        whatsAppService.sendMessage(from, "📊 *Categoria onde mais gastas:*\n\n🏆 " + top);
    }

    private void sendPiorDia(User user, String from) {
        List<Transaction> txs = transactionRepository.findByUserIdOrderByDataDesc(user.getId());
        Map<LocalDate, Double> porDia = new java.util.HashMap<>();

        txs.stream()
                .filter(t -> "DESPESA".equals(t.getTipo()))
                .forEach(t -> porDia.merge(t.getData(), t.getValor().doubleValue(), Double::sum));

        if (porDia.isEmpty()) {
            whatsAppService.sendMessage(from, "📅 Nenhuma despesa registada ainda.");
            return;
        }

        porDia.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(pior -> whatsAppService.sendMessage(from,
                        "📅 *Teu pior dia de gastos:*\n\n📆 " + pior.getKey() +
                                "\n💸 R$ " + String.format("%.2f", pior.getValue())
                ));
    }

    private void sendInsight(User user, String from) {
        double receitas = getSumByTipo(user, "RECEITA");
        double despesas = getSumByTipo(user, "DESPESA");
        double taxa = receitas > 0 ? (despesas / receitas) * 100 : 0;

        String msg;

        if (taxa < 50) {
            msg = "🌟 *Excelente!* Estás a gastar apenas " + String.format("%.0f", taxa) + "% das receitas!";
        } else if (taxa < 80) {
            msg = "👍 *Bom ritmo!* Estás a gastar " + String.format("%.0f", taxa) + "% das receitas. Podes poupar mais.";
        } else if (taxa < 100) {
            msg = "⚠️ *Atenção!* Estás a gastar " + String.format("%.0f", taxa) + "% das receitas. Corta gastos supérfluos.";
        } else {
            msg = "🚨 *Cuidado!* Despesas superam receitas. Revê os gastos urgentemente!";
        }

        whatsAppService.sendMessage(from, msg);
    }

    private void sendGastoPorCategoria(User user, String from, String msg) {
        String categoria = null;

        if (msg.contains("uber") || msg.contains("transporte")) {
            categoria = "Transporte";
        } else if (msg.contains("mercado") || msg.contains("alimentação")) {
            categoria = "Alimentação";
        } else if (msg.contains("lazer") || msg.contains("cinema")) {
            categoria = "Lazer";
        } else if (msg.contains("saúde") || msg.contains("farmácia")) {
            categoria = "Saúde";
        }

        if (categoria == null) {
            whatsAppService.sendMessage(from, "❓ Qual categoria? Ex: \"quanto gastei com uber?\"");
            return;
        }

        final String cat = categoria;

        double total = transactionRepository.findByUserIdOrderByDataDesc(user.getId())
                .stream()
                .filter(t -> "DESPESA".equals(t.getTipo()) && cat.equals(t.getCategoria()))
                .mapToDouble(t -> t.getValor().doubleValue())
                .sum();

        whatsAppService.sendMessage(from,
                "📊 *Gastos com " + cat + ":*\n\n💸 Total: *R$ " + String.format("%.2f", total) + "*"
        );
    }

    private void checkBudgetAlert(User user, String from, String categoria) {
        double totalCat = transactionRepository.findByUserIdOrderByDataDesc(user.getId())
                .stream()
                .filter(t -> "DESPESA".equals(t.getTipo()) && categoria.equals(t.getCategoria()))
                .mapToDouble(t -> t.getValor().doubleValue())
                .sum();

        Map<String, Double> limites = Map.of(
                "Alimentação", 800.0,
                "Transporte", 400.0,
                "Lazer", 500.0,
                "Moradia", 1200.0,
                "Saúde", 300.0
        );

        Double limite = limites.get(categoria);

        if (limite != null) {
            double pct = (totalCat / limite) * 100;

            if (pct >= 100) {
                whatsAppService.sendMessage(from,
                        "🚨 *Limite ultrapassado!*\n\n" +
                                "Ultrapassaste o orçamento de *" + categoria + "*!\n" +
                                "Gasto: R$ " + String.format("%.2f", totalCat) +
                                " | Limite: R$ " + limite.intValue()
                );
            } else if (pct >= 90) {
                whatsAppService.sendMessage(from,
                        "⚠️ *Alerta!* Já usaste " + String.format("%.0f", pct) +
                                "% do orçamento de *" + categoria + "*"
                );
            }
        }
    }

    private void sendAjuda(String from, String nome) {
        whatsAppService.sendMessage(from,
                "👋 Olá, *" + nome + "*! Comandos do Moneto:\n\n" +
                        "💸 *Despesa:* \"gastei 50 no mercado\"\n" +
                        "💰 *Receita:* \"recebi 3000 de salário\"\n" +
                        "📊 *Resumo:* \"resumo do mês\"\n" +
                        "📅 *Hoje:* \"quanto gastei hoje?\"\n" +
                        "💳 *Saldo:* \"meu saldo\"\n" +
                        "📋 *Últimos:* \"mostra meus gastos\"\n" +
                        "📊 *Categoria:* \"quanto gastei com uber?\"\n" +
                        "✏️ *Corrigir:* \"corrige para 30 reais\"\n" +
                        "🗑️ *Apagar:* \"apaga o último gasto\"\n" +
                        "💡 *Insight:* \"como estou financeiramente?\"\n" +
                        "❓ *Ajuda:* \"ajuda\""
        );
    }

    private double calcSaldo(User user) {
        return getSumByTipo(user, "RECEITA") - getSumByTipo(user, "DESPESA");
    }

    private double getSumByTipo(User user, String tipo) {
        Double val = transactionRepository.sumByUserIdAndTipo(user.getId(), tipo);
        return val != null ? val : 0.0;
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");

        if (!digits.startsWith("55") && digits.length() <= 11) {
            digits = "55" + digits;
        }

        return digits;
    }

    private String mapCategoria(String input) {
        if (input.contains("aliment") || input.contains("mercado") || input.contains("comida")) return "Alimentação";
        if (input.contains("transport") || input.contains("uber") || input.contains("gasolina")) return "Transporte";
        if (input.contains("lazer") || input.contains("cinema") || input.contains("netflix")) return "Lazer";
        if (input.contains("saúde") || input.contains("médico") || input.contains("farmácia")) return "Saúde";
        if (input.contains("moradia") || input.contains("aluguel") || input.contains("luz")) return "Moradia";
        if (input.contains("educação") || input.contains("curso")) return "Educação";
        return "Outros";
    }

    private boolean isAjudaCommand(String m) {
        return m.equals("ajuda") || m.equals("help") || m.equals("?");
    }

    private boolean isResumoMesCommand(String m) {
        return m.contains("resumo") || m.equals("r");
    }

    private boolean isResumoHojeCommand(String m) {
        return m.contains("hoje") && m.contains("gast");
    }

    private boolean isSaldoCommand(String m) {
        return m.contains("saldo") || m.contains("quanto tenho");
    }

    private boolean isUltimosCommand(String m) {
        return m.contains("últimos") || m.contains("ultimos") || m.contains("mostra");
    }

    private boolean isOrcamentoCommand(String m) {
        return m.contains("orçamento") || m.contains("posso gastar");
    }

    private boolean isMetaCommand(String m) {
        return m.contains("meta") && m.contains("falta");
    }

    private boolean isCorrecaoCommand(String m) {
        return m.contains("corrige") || m.contains("corrigi");
    }

    private boolean isApagaCommand(String m) {
        return m.contains("apaga") || m.contains("apagar");
    }

    private boolean isCategoriaCommand(String m) {
        return m.contains("onde gasto mais") || m.contains("categoria");
    }

    private boolean isPiorDiaCommand(String m) {
        return m.contains("pior dia");
    }

    private boolean isInsightCommand(String m) {
        return m.contains("economizar") || m.contains("como estou");
    }

    private boolean isGastoCategoria(String m) {
        return m.contains("quanto gastei com");
    }
}