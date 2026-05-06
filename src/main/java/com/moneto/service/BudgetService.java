package com.moneto.service;

import com.moneto.entity.Budget;
import com.moneto.entity.Category;
import com.moneto.entity.User;
import com.moneto.repository.BudgetRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository repo;
    private final UserRepository userRepo;
    private final CategoryService categoryService;

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Alimentação",
            "Transporte",
            "Moradia",
            "Saúde",
            "Educação",
            "Lazer",
            "Tecnologia",
            "Vestuário",
            "Outros"
    );

    public BudgetService(BudgetRepository repo, UserRepository userRepo, CategoryService categoryService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.categoryService = categoryService;
    }

    public List<Budget> getAll(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();

        categoryService.ensureDefaultCategories(user);

        for (String categoria : DEFAULT_CATEGORIES) {
            repo.findByUserIdAndCategoriaIgnoreCase(user.getId(), categoria)
                    .orElseGet(() -> {
                        Budget budget = new Budget();
                        budget.setCategoria(categoria);
                        budget.setLimite(BigDecimal.ZERO);
                        budget.setIcone(null);
                        budget.setUser(user);
                        return repo.save(budget);
                    });
        }

        return repo.findByUserId(user.getId());
    }

    public Budget create(String email, Budget b) {
        User user = userRepo.findByEmail(email).orElseThrow();

        String categoria = b.getCategoria() != null ? b.getCategoria().trim() : "";

        if (categoria.isBlank()) {
            throw new RuntimeException("Categoria é obrigatória.");
        }

        Category category = categoryService.findOrCreate(user, categoria, "DESPESA", b.getIcone());
        String categoriaFinal = category.getNome();

        if (repo.findByUserIdAndCategoriaIgnoreCase(user.getId(), categoriaFinal).isPresent()) {
            throw new RuntimeException("Já existe um orçamento para essa categoria.");
        }

        Budget budget = new Budget();
        budget.setCategoria(categoriaFinal);
        budget.setLimite(b.getLimite() != null ? b.getLimite() : BigDecimal.ZERO);
        budget.setIcone(b.getIcone());
        budget.setUser(user);

        return repo.save(budget);
    }

    public Budget update(String email, Long id, Budget b) {
        User user = userRepo.findByEmail(email).orElseThrow();

        Budget existing = repo.findById(id).orElseThrow();

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar este orçamento.");
        }

        String categoria = b.getCategoria() != null ? b.getCategoria().trim() : "";

        if (categoria.isBlank()) {
            throw new RuntimeException("Categoria é obrigatória.");
        }

        Category category = categoryService.findOrCreate(user, categoria, "DESPESA", b.getIcone());
        String categoriaFinal = category.getNome();

        repo.findByUserIdAndCategoriaIgnoreCase(user.getId(), categoriaFinal)
                .ifPresent(other -> {
                    if (!other.getId().equals(id)) {
                        throw new RuntimeException("Já existe um orçamento para essa categoria.");
                    }
                });

        existing.setCategoria(categoriaFinal);
        existing.setLimite(b.getLimite() != null ? b.getLimite() : BigDecimal.ZERO);
        existing.setIcone(b.getIcone());

        return repo.save(existing);
    }

    public void delete(String email, Long id) {
        User user = userRepo.findByEmail(email).orElseThrow();

        Budget existing = repo.findById(id).orElseThrow();

        if (!existing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir este orçamento.");
        }

        if (DEFAULT_CATEGORIES.stream().anyMatch(c -> c.equalsIgnoreCase(existing.getCategoria()))) {
            existing.setLimite(BigDecimal.ZERO);
            repo.save(existing);
            return;
        }

        repo.delete(existing);
    }
}