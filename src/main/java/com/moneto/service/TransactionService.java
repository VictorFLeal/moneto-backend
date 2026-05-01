package com.moneto.service;

import com.moneto.dto.TransactionDTO;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<TransactionDTO> findAll(String email) {
        User user = getUser(email);
        return transactionRepository
                .findByUserIdOrderByDataDesc(user.getId())
                .stream().map(this::toDTO).toList();
    }

    public TransactionDTO create(String email, TransactionDTO dto) {
        User user = getUser(email);
        Transaction tx = Transaction.builder()
                .descricao(dto.getDescricao())
                .valor(dto.getValor())
                .tipo(dto.getTipo())
                .categoria(dto.getCategoria())
                .data(dto.getData())
                .origem(dto.getOrigem() != null ? dto.getOrigem() : "manual")
                .user(user)
                .build();
        return toDTO(transactionRepository.save(tx));
    }

    public TransactionDTO update(String email, Long id, TransactionDTO dto) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        tx.setDescricao(dto.getDescricao());
        tx.setValor(dto.getValor());
        tx.setTipo(dto.getTipo());
        tx.setCategoria(dto.getCategoria());
        tx.setData(dto.getData());
        return toDTO(transactionRepository.save(tx));
    }

    public void delete(String email, Long id) {
        transactionRepository.deleteById(id);
    }

    public Map<String, Object> getSummary(String email) {
        User user = getUser(email);
        Double receitas  = transactionRepository.sumByUserIdAndTipo(user.getId(), "RECEITA");
        Double despesas  = transactionRepository.sumByUserIdAndTipo(user.getId(), "DESPESA");
        receitas  = receitas  != null ? receitas  : 0.0;
        despesas  = despesas  != null ? despesas  : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("receitas", receitas);
        summary.put("despesas", despesas);
        summary.put("saldo", receitas - despesas);
        return summary;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private TransactionDTO toDTO(Transaction tx) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(tx.getId());
        dto.setDescricao(tx.getDescricao());
        dto.setValor(tx.getValor());
        dto.setTipo(tx.getTipo());
        dto.setCategoria(tx.getCategoria());
        dto.setData(tx.getData());
        dto.setOrigem(tx.getOrigem());
        return dto;
    }
}