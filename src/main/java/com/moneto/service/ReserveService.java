package com.moneto.service;

import com.moneto.dto.ReserveDTO;
import com.moneto.entity.Reserve;
import com.moneto.entity.User;
import com.moneto.repository.ReserveRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReserveService {

    private final ReserveRepository reserveRepository;
    private final UserRepository userRepository;

    public ReserveService(ReserveRepository reserveRepository, UserRepository userRepository) {
        this.reserveRepository = reserveRepository;
        this.userRepository = userRepository;
    }

    public List<ReserveDTO> findAll(String email) {
        User user = getUser(email);

        return reserveRepository.findByUserId(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ReserveDTO create(String email, ReserveDTO dto) {
        User user = getUser(email);

        Reserve reserve = new Reserve();
        reserve.setNome(dto.getNome());
        reserve.setCategoria(dto.getCategoria());
        reserve.setValorAtual(dto.getValorAtual() != null ? dto.getValorAtual() : BigDecimal.ZERO);
        reserve.setMeta(dto.getMeta());
        reserve.setUser(user);

        return toDTO(reserveRepository.save(reserve));
    }

    public ReserveDTO update(String email, Long id, ReserveDTO dto) {
        User user = getUser(email);

        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

        if (!reserve.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar esta reserva.");
        }

        reserve.setNome(dto.getNome());
        reserve.setCategoria(dto.getCategoria());
        reserve.setMeta(dto.getMeta());

        return toDTO(reserveRepository.save(reserve));
    }

    public void delete(String email, Long id) {
        User user = getUser(email);

        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

        if (!reserve.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir esta reserva.");
        }

        reserveRepository.delete(reserve);
    }

    public ReserveDTO addValue(String email, Long id, BigDecimal valor) {
        User user = getUser(email);

        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

        if (!reserve.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar esta reserva.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor precisa ser maior que zero.");
        }

        BigDecimal valorAtual = reserve.getValorAtual() != null ? reserve.getValorAtual() : BigDecimal.ZERO;
        reserve.setValorAtual(valorAtual.add(valor));

        return toDTO(reserveRepository.save(reserve));
    }

    public ReserveDTO withdrawValue(String email, Long id, BigDecimal valor) {
        User user = getUser(email);

        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

        if (!reserve.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar esta reserva.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor precisa ser maior que zero.");
        }

        BigDecimal valorAtual = reserve.getValorAtual() != null ? reserve.getValorAtual() : BigDecimal.ZERO;

        if (valorAtual.compareTo(valor) < 0) {
            throw new RuntimeException("Não é possível retirar um valor maior que o saldo da reserva.");
        }

        reserve.setValorAtual(valorAtual.subtract(valor));

        return toDTO(reserveRepository.save(reserve));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private ReserveDTO toDTO(Reserve r) {
        ReserveDTO dto = new ReserveDTO();
        dto.setId(r.getId());
        dto.setNome(r.getNome());
        dto.setCategoria(r.getCategoria());
        dto.setValorAtual(r.getValorAtual());
        dto.setMeta(r.getMeta());
        return dto;
    }
}