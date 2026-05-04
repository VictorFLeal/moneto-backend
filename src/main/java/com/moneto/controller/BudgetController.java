package com.moneto.controller;

import com.moneto.entity.Budget;
import com.moneto.service.BudgetService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public List<Budget> getAll(@AuthenticationPrincipal UserDetails user) {
        return service.getAll(user.getUsername());
    }

    @PostMapping
    public Budget create(@AuthenticationPrincipal UserDetails user, @RequestBody Budget b) {
        return service.create(user.getUsername(), b);
    }

    @PutMapping("/{id}")
    public Budget update(@AuthenticationPrincipal UserDetails user, @PathVariable Long id, @RequestBody Budget b) {
        return service.update(user.getUsername(), id, b);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        service.delete(user.getUsername(), id);
    }
}