package com.moneto.controller;

import com.moneto.dto.DebtDTO;
import com.moneto.service.DebtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    @GetMapping
    public ResponseEntity<List<DebtDTO>> getAll(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(debtService.findAll(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<DebtDTO> create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody DebtDTO dto) {

        return ResponseEntity.ok(debtService.create(user.getUsername(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DebtDTO> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody DebtDTO dto) {

        return ResponseEntity.ok(debtService.update(user.getUsername(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        debtService.delete(id);
        return ResponseEntity.noContent().build();
    }
}