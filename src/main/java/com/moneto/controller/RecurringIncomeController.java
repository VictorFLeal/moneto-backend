package com.moneto.controller;

import com.moneto.entity.RecurringIncome;
import com.moneto.service.RecurringIncomeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-incomes")
public class RecurringIncomeController {

    private final RecurringIncomeService service;

    public RecurringIncomeController(RecurringIncomeService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecurringIncome> getAll(@AuthenticationPrincipal UserDetails user) {
        return service.getAll(user.getUsername());
    }

    @PostMapping
    public RecurringIncome create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody RecurringIncome income
    ) {
        return service.create(user.getUsername(), income);
    }

    @PutMapping("/{id}")
    public RecurringIncome update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody RecurringIncome income
    ) {
        return service.update(user.getUsername(), id, income);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id
    ) {
        service.delete(user.getUsername(), id);
    }
}