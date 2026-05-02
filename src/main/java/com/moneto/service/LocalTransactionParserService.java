package com.moneto.service;

import com.moneto.dto.ParsedTransactionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalTransactionParserService {

    public ParsedTransactionDTO parseMessage(String message) {
        ParsedTransactionDTO dto = new ParsedTransactionDTO();
        dto.setMensagemOriginal(message);

        if (message == null || message.isBlank()) {
            dto.setParsed(false);
            dto.setErro("Mensagem vazia.");
            return dto;
        }

        String msg = message.toLowerCase().trim();

        BigDecimal valor = extrairValor(msg);

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            dto.setParsed(false);
            dto.setErro("Não encontrei um valor na mensagem.");
            return dto;
        }

        String tipo = detectarTipo(msg);
        String categoria = detectarCategoria(msg);
        String descricao = limparDescricao(msg);

        if (descricao.isBlank()) {
            descricao = categoria;
        }

        dto.setParsed(true);
        dto.setValor(valor);
        dto.setTipo(tipo);
        dto.setCategoria(categoria);
        dto.setDescricao(descricao);
        dto.setData(LocalDate.now());
        dto.setErro(null);

        return dto;
    }

    private BigDecimal extrairValor(String msg) {
        Pattern pattern = Pattern.compile("(\\d+[\\.,]?\\d*)");
        Matcher matcher = pattern.matcher(msg);

        if (matcher.find()) {
            String numero = matcher.group(1).replace(",", ".");
            return new BigDecimal(numero);
        }

        return null;
    }

    private String detectarTipo(String msg) {
        if (msg.contains("recebi")
                || msg.contains("ganhei")
                || msg.contains("salário")
                || msg.contains("salario")
                || msg.contains("freela")
                || msg.contains("freelance")
                || msg.contains("pix recebido")) {
            return "RECEITA";
        }

        return "DESPESA";
    }

    private String detectarCategoria(String msg) {
        if (msg.contains("mercado") || msg.contains("ifood") || msg.contains("comida")
                || msg.contains("restaurante") || msg.contains("lanche")
                || msg.contains("padaria") || msg.contains("pizza")) {
            return "Alimentação";
        }

        if (msg.contains("uber") || msg.contains("99") || msg.contains("ônibus")
                || msg.contains("onibus") || msg.contains("gasolina")
                || msg.contains("transporte")) {
            return "Transporte";
        }

        if (msg.contains("luz") || msg.contains("água") || msg.contains("agua")
                || msg.contains("internet") || msg.contains("aluguel")
                || msg.contains("casa")) {
            return "Moradia";
        }

        if (msg.contains("remédio") || msg.contains("remedio")
                || msg.contains("farmácia") || msg.contains("farmacia")
                || msg.contains("médico") || msg.contains("medico")
                || msg.contains("consulta")) {
            return "Saúde";
        }

        if (msg.contains("curso") || msg.contains("faculdade")
                || msg.contains("senac") || msg.contains("livro")
                || msg.contains("educação") || msg.contains("educacao")) {
            return "Educação";
        }

        if (msg.contains("netflix") || msg.contains("cinema")
                || msg.contains("jogo") || msg.contains("lazer")
                || msg.contains("show")) {
            return "Lazer";
        }

        if (msg.contains("salário") || msg.contains("salario")) {
            return "Salário";
        }

        if (msg.contains("freela") || msg.contains("freelance")) {
            return "Freelance";
        }

        return "Outros";
    }

    private String limparDescricao(String msg) {
        return msg
                .replaceAll("\\d+[\\.,]?\\d*", "")
                .replace("gastei", "")
                .replace("paguei", "")
                .replace("comprei", "")
                .replace("recebi", "")
                .replace("ganhei", "")
                .replace("reais", "")
                .replace("real", "")
                .replace("r$", "")
                .replace("no", "")
                .replace("na", "")
                .replace("com", "")
                .replace("de", "")
                .trim();
    }
}