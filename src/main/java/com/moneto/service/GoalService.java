package com.moneto.service;

import com.moneto.dto.GoalDTO;
import com.moneto.entity.*;
import com.moneto.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public List<GoalDTO> findAll(String email) {
        User user = getUser(email);
        return goalRepository.findByUserId(user.getId())
                .stream().map(this::toDTO).toList();
    }

    public GoalDTO create(String email, GoalDTO dto) {
        User user = getUser(email);
        Goal goal = Goal.builder()
                .titulo(dto.getTitulo())
                .icone(dto.getIcone())
                .valorMeta(dto.getValorMeta())
                .valorAtual(dto.getValorAtual())
                .prazo(dto.getPrazo())
                .user(user)
                .build();
        return toDTO(goalRepository.save(goal));
    }

    public GoalDTO update(String email, Long id, GoalDTO dto) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));
        goal.setTitulo(dto.getTitulo());
        goal.setValorMeta(dto.getValorMeta());
        goal.setValorAtual(dto.getValorAtual());
        goal.setPrazo(dto.getPrazo());
        goal.setStatus(dto.getStatus());
        return toDTO(goalRepository.save(goal));
    }

    public void delete(Long id) {
        goalRepository.deleteById(id);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private GoalDTO toDTO(Goal g) {
        GoalDTO dto = new GoalDTO();
        dto.setId(g.getId());
        dto.setTitulo(g.getTitulo());
        dto.setIcone(g.getIcone());
        dto.setValorMeta(g.getValorMeta());
        dto.setValorAtual(g.getValorAtual());
        dto.setPrazo(g.getPrazo());
        dto.setStatus(g.getStatus());
        return dto;
    }
}