package com.moneto.service;

import com.moneto.entity.Budget;
import com.moneto.entity.User;
import com.moneto.repository.BudgetRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository repo;
    private final UserRepository userRepo;

    public BudgetService(BudgetRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public List<Budget> getAll(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return repo.findByUserId(user.getId());
    }

    public Budget create(String email, Budget b) {
        User user = userRepo.findByEmail(email).orElseThrow();
        b.setUser(user);
        return repo.save(b);
    }

    public Budget update(String email, Long id, Budget b) {
        Budget existing = repo.findById(id).orElseThrow();
        existing.setCategoria(b.getCategoria());
        existing.setLimite(b.getLimite());
        return repo.save(existing);
    }

    public void delete(String email, Long id) {
        repo.deleteById(id);
    }
}