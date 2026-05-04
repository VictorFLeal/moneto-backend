package com.moneto.controller;

import com.moneto.dto.UpdatePlanRequest;
import com.moneto.entity.User;
import com.moneto.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserPlanController {

    private final UserRepository userRepository;

    public UserPlanController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/plan")
    public ResponseEntity<?> updatePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdatePlanRequest request
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String plano = request.getPlano();

        if (plano == null || plano.isBlank()) {
            throw new RuntimeException("Plano inválido.");
        }

        plano = plano.toLowerCase();

        if (!plano.equals("start")
                && !plano.equals("essencial")
                && !plano.equals("pro")
                && !plano.equals("business")) {
            throw new RuntimeException("Plano inválido.");
        }

        user.setPlano(plano);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Plano atualizado com sucesso.",
                "plano", user.getPlano()
        ));
    }
}