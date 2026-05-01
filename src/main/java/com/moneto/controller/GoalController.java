package com.moneto.controller;

import com.moneto.dto.GoalDTO;
import com.moneto.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}