package com.moneto.service;

import com.moneto.dto.DebtDTO;
import com.moneto.entity.Debt;
import com.moneto.entity.User;
import com.moneto.repository.DebtRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final PlanAccessService planAccessService;

    public DebtService(
            DebtRepository debtRepository,
            UserRepository userRepository,
            PlanAccessService planAccessService
    ) {
        this.debtRepository = debtRepository;
        this.userRepository = userRepository;
        this.planAccessService = planAccessService;
    }

    public List<DebtDTO> findAll(String email) {
        User user = getUser(email);

        planAccessService.validarModoDividas(user);

        return debtRepository.findByUserId(user.getId())
                .stream()
                .map(d -> toDTO(d))
                .toList();
    }

    public DebtDTO create(String email, DebtDTO dto) {
        User user = getUser(email);

        planAccessService.validarModoDividas(user);

        Debt debt = new Debt();
        debt.setNome(dto.getNome());
        debt.setValorTotal(dto.getValorTotal());
        debt.setValorPago(dto.getValorPago());
        debt.setTaxaJuros(dto.getTaxaJuros());
        debt.setPagamentoMinimo(dto.getPagamentoMinimo());
        debt.setVencimento(dto.getVencimento());
        debt.setStatus(dto.getStatus());
        debt.setUser(user);

        return toDTO(debtRepository.save(debt));
    }

    public DebtDTO update(String email, Long id, DebtDTO dto) {
        User user = getUser(email);

        planAccessService.validarModoDividas(user);

        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dívida não encontrada"));

        if (!debt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar esta dívida.");
        }

        debt.setValorPago(dto.getValorPago());
        debt.setStatus(dto.getStatus());

        return toDTO(debtRepository.save(debt));
    }

    public void delete(String email, Long id) {
        User user = getUser(email);

        planAccessService.validarModoDividas(user);

        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dívida não encontrada"));

        if (!debt.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir esta dívida.");
        }

        debtRepository.deleteById(id);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private DebtDTO toDTO(Debt d) {
        DebtDTO dto = new DebtDTO();

        dto.setId(d.getId());
        dto.setNome(d.getNome());
        dto.setValorTotal(d.getValorTotal());
        dto.setValorPago(d.getValorPago());
        dto.setTaxaJuros(d.getTaxaJuros());
        dto.setPagamentoMinimo(d.getPagamentoMinimo());
        dto.setVencimento(d.getVencimento());
        dto.setStatus(d.getStatus());

        return dto;
    }
}