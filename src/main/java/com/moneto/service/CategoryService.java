package com.moneto.service;

import com.moneto.entity.Category;
import com.moneto.entity.User;
import com.moneto.repository.CategoryRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private static final List<CategorySeed> DEFAULT_CATEGORIES = List.of(
            new CategorySeed("Alimentação", "DESPESA", "🛒"),
            new CategorySeed("Transporte", "DESPESA", "🚗"),
            new CategorySeed("Moradia", "DESPESA", "🏠"),
            new CategorySeed("Saúde", "DESPESA", "💊"),
            new CategorySeed("Educação", "DESPESA", "📚"),
            new CategorySeed("Lazer", "DESPESA", "🎬"),
            new CategorySeed("Tecnologia", "DESPESA", "💻"),
            new CategorySeed("Vestuário", "DESPESA", "👕"),
            new CategorySeed("Outros", "DESPESA", "📦"),
            new CategorySeed("Salário", "RECEITA", "💼"),
            new CategorySeed("Freelance", "RECEITA", "💰")
    );

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<Category> getAll(String email) {
        User user = getUser(email);
        ensureDefaultCategories(user);
        return categoryRepository.findByUserIdOrderByNomeAsc(user.getId());
    }

    public Category create(String email, Category category) {
        User user = getUser(email);

        String nome = normalizeName(category.getNome());

        if (nome.isBlank()) {
            throw new RuntimeException("Nome da categoria é obrigatório.");
        }

        return categoryRepository.findByUserIdAndNomeIgnoreCase(user.getId(), nome)
                .orElseGet(() -> {
                    Category nova = new Category();
                    nova.setNome(nome);
                    nova.setTipo(normalizeType(category.getTipo()));
                    nova.setIcone(category.getIcone());
                    nova.setPadrao(false);
                    nova.setUser(user);
                    return categoryRepository.save(nova);
                });
    }

    public Category findOrCreate(String email, String nome, String tipo, String icone) {
        User user = getUser(email);
        return findOrCreate(user, nome, tipo, icone);
    }

    public Category findOrCreate(User user, String nome, String tipo, String icone) {
        String normalizedName = normalizeName(nome);

        if (normalizedName.isBlank()) {
            throw new RuntimeException("Categoria é obrigatória.");
        }

        ensureDefaultCategories(user);

        return categoryRepository.findByUserIdAndNomeIgnoreCase(user.getId(), normalizedName)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setNome(normalizedName);
                    category.setTipo(normalizeType(tipo));
                    category.setIcone(icone);
                    category.setPadrao(false);
                    category.setUser(user);
                    return categoryRepository.save(category);
                });
    }

    public void ensureDefaultCategories(User user) {
        for (CategorySeed seed : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsByUserIdAndNomeIgnoreCase(user.getId(), seed.nome())) {
                Category category = new Category();
                category.setNome(seed.nome());
                category.setTipo(seed.tipo());
                category.setIcone(seed.icone());
                category.setPadrao(true);
                category.setUser(user);
                categoryRepository.save(category);
            }
        }
    }

    public String getCanonicalCategoryName(User user, String nome, String tipo) {
        Category category = findOrCreate(user, nome, tipo, null);
        return category.getNome();
    }

    private String normalizeName(String nome) {
        if (nome == null) return "";
        String trimmed = nome.trim();

        return trimmed
                .replaceAll("\\s+", " ")
                .substring(0, 1)
                .toUpperCase() + trimmed.replaceAll("\\s+", " ").substring(1);
    }

    private String normalizeType(String tipo) {
        if (tipo == null || tipo.isBlank()) return "DESPESA";
        return tipo.trim().toUpperCase();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private record CategorySeed(String nome, String tipo, String icone) {}
}