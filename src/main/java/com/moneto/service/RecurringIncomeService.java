package com.moneto.service;

import com.moneto.entity.RecurringIncome;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.RecurringIncomeRepository;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringIncomeService {

    private final RecurringIncomeRepository recurringIncomeRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public RecurringIncomeService(
            RecurringIncomeRepository recurringIncomeRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            CategoryService categoryService
    ) {
        this.recurringIncomeRepository = recurringIncomeRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    public List<RecurringIncome> getAll(String email) {
        User user = getUser(email);
        return recurringIncomeRepository.findByUserIdOrderByIdDesc(user.getId());
    }

    public RecurringIncome create(String email, RecurringIncome income) {
        User user = getUser(email);

        if (income.getDescricao() == null || income.getDescricao().trim().isBlank()) {
            throw new RuntimeException("Descrição é obrigatória.");
        }

        if (income.getValor() == null || income.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor deve ser maior que zero.");
        }

        String categoria = income.getCategoria() == null || income.getCategoria().isBlank()
                ? "Salário"
                : income.getCategoria().trim();

        categoryService.findOrCreate(user, categoria, "RECEITA", "💼");

        RecurringIncome novo = new RecurringIncome();
        novo.setDescricao(income.getDescricao().trim());
        novo.setValor(income.getValor());
        novo.setCategoria(categoria);
        novo.setRegraLancamento(normalizeRule(income.getRegraLancamento()));
        novo.setDiaFixo(income.getDiaFixo());
        novo.setAtivo(income.getAtivo() == null || income.getAtivo());
        novo.setUser(user);

        return recurringIncomeRepository.save(novo);
    }

    public RecurringIncome update(String email, Long id, RecurringIncome income) {
        User user = getUser(email);

        RecurringIncome existing = recurringIncomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ganho fixo não encontrado."));

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar este ganho fixo.");
        }

        if (income.getDescricao() == null || income.getDescricao().trim().isBlank()) {
            throw new RuntimeException("Descrição é obrigatória.");
        }

        if (income.getValor() == null || income.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor deve ser maior que zero.");
        }

        String categoria = income.getCategoria() == null || income.getCategoria().isBlank()
                ? "Salário"
                : income.getCategoria().trim();

        categoryService.findOrCreate(user, categoria, "RECEITA", "💼");

        existing.setDescricao(income.getDescricao().trim());
        existing.setValor(income.getValor());
        existing.setCategoria(categoria);
        existing.setRegraLancamento(normalizeRule(income.getRegraLancamento()));
        existing.setDiaFixo(income.getDiaFixo());
        existing.setAtivo(income.getAtivo() == null || income.getAtivo());

        return recurringIncomeRepository.save(existing);
    }

    public void delete(String email, Long id) {
        User user = getUser(email);

        RecurringIncome existing = recurringIncomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ganho fixo não encontrado."));

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir este ganho fixo.");
        }

        recurringIncomeRepository.delete(existing);
    }

    public void processMonthlyIncomes(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startMonth = today.withDayOfMonth(1);
        LocalDate endMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<RecurringIncome> incomes = recurringIncomeRepository.findByUserIdAndAtivoTrue(user.getId());

        for (RecurringIncome income : incomes) {
            LocalDate launchDate = getLaunchDate(today, income);

            if (today.isBefore(launchDate)) {
                continue;
            }

            boolean alreadyCreated = transactionRepository.existsByUserIdAndOrigemAndDescricaoAndDataBetween(
                    user.getId(),
                    "recorrente",
                    income.getDescricao(),
                    startMonth,
                    endMonth
            );

            if (alreadyCreated) {
                continue;
            }

            String categoriaFinal = categoryService.getCanonicalCategoryName(
                    user,
                    income.getCategoria(),
                    "RECEITA"
            );

            Transaction tx = new Transaction();
            tx.setDescricao(income.getDescricao());
            tx.setValor(income.getValor());
            tx.setTipo("RECEITA");
            tx.setCategoria(categoriaFinal);
            tx.setData(launchDate);
            tx.setOrigem("recorrente");
            tx.setUser(user);

            transactionRepository.save(tx);
        }
    }

    private LocalDate getLaunchDate(LocalDate today, RecurringIncome income) {
        String rule = normalizeRule(income.getRegraLancamento());

        if ("DIA_FIXO".equals(rule)) {
            int day = income.getDiaFixo() == null ? 5 : income.getDiaFixo();
            int safeDay = Math.max(1, Math.min(day, today.lengthOfMonth()));
            return today.withDayOfMonth(safeDay);
        }

        return getFifthBusinessDay(today);
    }

    private LocalDate getFifthBusinessDay(LocalDate date) {
        LocalDate cursor = date.withDayOfMonth(1);
        int businessDays = 0;

        while (cursor.getMonth() == date.getMonth()) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                businessDays++;

                if (businessDays == 5) {
                    return cursor;
                }
            }

            cursor = cursor.plusDays(1);
        }

        return date.withDayOfMonth(5);
    }

    private String normalizeRule(String rule) {
        if (rule == null || rule.isBlank()) {
            return "QUINTO_DIA_UTIL";
        }

        return rule.trim().toUpperCase();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }
}