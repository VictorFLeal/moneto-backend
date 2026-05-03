package com.moneto.controller;

import com.moneto.dto.GoalDTO;
import com.moneto.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public ResponseEntity<List<GoalDTO>> getAll(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(goalService.findAll(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<GoalDTO> create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody GoalDTO dto) {

        return ResponseEntity.ok(goalService.create(user.getUsername(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalDTO> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody GoalDTO dto) {

        return ResponseEntity.ok(goalService.update(user.getUsername(), id, dto));
    }

    @PostMapping("/{id}/add-value")
    public ResponseEntity<GoalDTO> addValue(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal valor = body.get("valor");

        return ResponseEntity.ok(goalService.addValue(user.getUsername(), id, valor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        goalService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}