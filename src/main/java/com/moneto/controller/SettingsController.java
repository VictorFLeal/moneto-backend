package com.moneto.controller;

import com.moneto.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> get(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(settingsService.getSettings(user.getUsername()));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> update(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(settingsService.updateSettings(user.getUsername(), body));
    }
}