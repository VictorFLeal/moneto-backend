package com.moneto.controller;

import com.moneto.dto.PlanDTO;
import com.moneto.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PlanDTO>> getAll() {
        return ResponseEntity.ok(planService.findAll());
    }
}