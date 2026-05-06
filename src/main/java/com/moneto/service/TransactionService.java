package com.moneto.service;

import com.moneto.dto.TransactionDTO;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final RecurringIncomeService recurringIncomeService;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            CategoryService categoryService,
            RecurringIncomeService recurringIncomeService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.recurringIncomeService = recurringIncomeService;
    }

    public List<TransactionDTO> findAll(String email) {
        User user = getUser(email);

        recurringIncomeService.processMonthlyIncomes(user);

        if (isStart(user)) {
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
            LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            return transactionRepository
                    .findByUserIdAndDataBetweenOrderByDataDesc(user.getId(), inicioMes, fimMes)
                    .stream()
                    .map(this::toDTO)
                    .toList();
        }

        return transactionRepository
                .findByUserIdOrderByDataDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public TransactionDTO create(String email, TransactionDTO dto) {
        User user = getUser(email);

        validarLimiteLancamentosStart(user);

        String categoriaFinal = categoryService.getCanonicalCategoryName(
                user,
                dto.getCategoria(),
                dto.getTipo()
        );

        Transaction tx = new Transaction();
        tx.setDescricao(dto.getDescricao());
        tx.setValor(dto.getValor());
        tx.setTipo(normalizeType(dto.getTipo()));
        tx.setCategoria(categoriaFinal);
        tx.setData(dto.getData() != null ? dto.getData() : LocalDate.now());
        tx.setOrigem(dto.getOrigem());
        tx.setUser(user);

        return toDTO(transactionRepository.save(tx));
    }

    public TransactionDTO update(String email, Long id, TransactionDTO dto) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        User user = getUser(email);

        if (!tx.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar esta transação.");
        }

        String categoriaFinal = categoryService.getCanonicalCategoryName(
                user,
                dto.getCategoria(),
                dto.getTipo()
        );

        tx.setDescricao(dto.getDescricao());
        tx.setValor(dto.getValor());
        tx.setTipo(normalizeType(dto.getTipo()));
        tx.setCategoria(categoriaFinal);
        tx.setData(dto.getData() != null ? dto.getData() : LocalDate.now());
        tx.setOrigem(dto.getOrigem());

        return toDTO(transactionRepository.save(tx));
    }

    public void delete(String email, Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        User user = getUser(email);

        if (!tx.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir esta transação.");
        }

        transactionRepository.deleteById(id);
    }

    public Map<String, Object> getSummary(String email) {
        User user = getUser(email);

        recurringIncomeService.processMonthlyIncomes(user);

        Double receitas = transactionRepository.sumByUserIdAndTipo(user.getId(), "RECEITA");
        Double despesas = transactionRepository.sumByUserIdAndTipo(user.getId(), "DESPESA");

        receitas = receitas != null ? receitas : 0.0;
        despesas = despesas != null ? despesas : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("receitas", receitas);
        summary.put("despesas", despesas);
        summary.put("saldo", receitas - despesas);

        if (isStart(user)) {
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
            LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            long lancamentosMes = transactionRepository
                    .countByUserIdAndDataBetween(user.getId(), inicioMes, fimMes);

            long whatsappMes = transactionRepository
                    .countByUserIdAndOrigemAndDataBetween(user.getId(), "whatsapp", inicioMes, fimMes);

            summary.put("plano", "start");
            summary.put("limiteLancamentos", 30);
            summary.put("lancamentosUsados", lancamentosMes);
            summary.put("limiteWhatsapp", 10);
            summary.put("whatsappUsado", whatsappMes);
        } else {
            summary.put("plano", user.getPlano());
            summary.put("limiteLancamentos", "ilimitado");
            summary.put("limiteWhatsapp", "ilimitado");
        }

        return summary;
    }

    private void validarLimiteLancamentosStart(User user) {
        if (!isStart(user)) {
            return;
        }

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        long totalLancamentos = transactionRepository
                .countByUserIdAndDataBetween(user.getId(), inicioMes, fimMes);

        if (totalLancamentos >= 30) {
            throw new RuntimeException("Seu plano Start permite até 30 lançamentos por mês.");
        }
    }

    private boolean isStart(User user) {
        return user.getPlano() == null || "start".equalsIgnoreCase(user.getPlano());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private String normalizeType(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "DESPESA";
        }

        return tipo.trim().toUpperCase();
    }

    private TransactionDTO toDTO(Transaction tx) {
        TransactionDTO dto = new TransactionDTO();

        dto.setId(tx.getId());
        dto.setDescricao(tx.getDescricao());
        dto.setValor(tx.getValor());
        dto.setTipo(tx.getTipo());
        dto.setCategoria(tx.getCategoria());
        dto.setData(tx.getData());
        dto.setOrigem(tx.getOrigem());

        return dto;
    }
}