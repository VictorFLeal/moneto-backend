package com.moneto.service;

import com.moneto.dto.GoalDTO;
import com.moneto.entity.Goal;
import com.moneto.entity.User;
import com.moneto.repository.GoalRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    public List<GoalDTO> findAll(String email) {
        User user = getUser(email);

        return goalRepository.findByUserId(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public GoalDTO create(String email, GoalDTO dto) {
        User user = getUser(email);

        Goal goal = new Goal();
        goal.setTitulo(dto.getTitulo());
        goal.setIcone(dto.getIcone());
        goal.setValorMeta(dto.getValorMeta());
        goal.setValorAtual(dto.getValorAtual());
        goal.setPrazo(dto.getPrazo());
        goal.setStatus(dto.getStatus());
        goal.setUser(user);

        return toDTO(goalRepository.save(goal));
    }

    public GoalDTO update(String email, Long id, GoalDTO dto) {
        User user = getUser(email);

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Meta não pertence ao utilizador");
        }

        goal.setTitulo(dto.getTitulo());
        goal.setIcone(dto.getIcone());
        goal.setValorMeta(dto.getValorMeta());
        goal.setValorAtual(dto.getValorAtual());
        goal.setPrazo(dto.getPrazo());
        goal.setStatus(dto.getStatus());

        return toDTO(goalRepository.save(goal));
    }

    public GoalDTO addValue(String email, Long id, BigDecimal valor) {
        User user = getUser(email);

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Meta não pertence ao utilizador");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Valor inválido");
        }

        BigDecimal atual = goal.getValorAtual() != null ? goal.getValorAtual() : BigDecimal.ZERO;
        BigDecimal meta = goal.getValorMeta() != null ? goal.getValorMeta() : BigDecimal.ZERO;

        BigDecimal novoValor = atual.add(valor);

        if (meta.compareTo(BigDecimal.ZERO) > 0 && novoValor.compareTo(meta) >= 0) {
            novoValor = meta;
            goal.setStatus("concluida");
        }

        goal.setValorAtual(novoValor);

        return toDTO(goalRepository.save(goal));
    }

    public void delete(String email, Long id) {
        User user = getUser(email);

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Meta não pertence ao utilizador");
        }

        goalRepository.delete(goal);
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