package com.moneto.controller;

import com.moneto.dto.ReserveDTO;
import com.moneto.service.ReserveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reserves")
public class ReserveController {

    private final ReserveService reserveService;

    public ReserveController(ReserveService reserveService) {
        this.reserveService = reserveService;
    }

    @GetMapping
    public ResponseEntity<List<ReserveDTO>> getAll(
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(reserveService.findAll(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ReserveDTO> create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody ReserveDTO dto) {

        return ResponseEntity.ok(reserveService.create(user.getUsername(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReserveDTO> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody ReserveDTO dto) {

        return ResponseEntity.ok(reserveService.update(user.getUsername(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        reserveService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ReserveDTO> deposit(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestParam BigDecimal valor) {

        return ResponseEntity.ok(reserveService.addValue(user.getUsername(), id, valor));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ReserveDTO> withdraw(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestParam BigDecimal valor) {

        return ResponseEntity.ok(reserveService.withdrawValue(user.getUsername(), id, valor));
    }
}