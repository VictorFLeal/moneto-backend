package com.moneto.controller;

import com.moneto.dto.BusinessEntryDTO;
import com.moneto.service.BusinessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/entries")
    public ResponseEntity<List<BusinessEntryDTO>> getAll(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(businessService.findAll(user.getUsername()));
    }

    @PostMapping("/entries")
    public ResponseEntity<BusinessEntryDTO> create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody BusinessEntryDTO dto
    ) {
        return ResponseEntity.ok(businessService.create(user.getUsername(), dto));
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<BusinessEntryDTO> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestBody BusinessEntryDTO dto
    ) {
        return ResponseEntity.ok(businessService.update(user.getUsername(), id, dto));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id
    ) {
        businessService.delete(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(businessService.summary(user.getUsername()));
    }
}