package com.moneto.controller;

import com.moneto.dto.TransactionDTO;
import com.moneto.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAll(
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(transactionService.findAll(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody TransactionDTO dto) {

        return ResponseEntity.ok(transactionService.create(user.getUsername(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody TransactionDTO dto) {

        return ResponseEntity.ok(transactionService.update(user.getUsername(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        transactionService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(transactionService.getSummary(user.getUsername()));
    }
}