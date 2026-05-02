package com.moneto.controller;

import com.moneto.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TransactionService transactionService;

    public DashboardController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(transactionService.getSummary(user.getUsername()));
    }
}