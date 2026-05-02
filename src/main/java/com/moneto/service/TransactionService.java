package com.moneto.service;

import com.moneto.dto.TransactionDTO;
import com.moneto.entity.Transaction;
import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public List<TransactionDTO> findAll(String email) {
        User user = getUser(email);

        return transactionRepository
                .findByUserIdOrderByDataDesc(user.getId())
                .stream()
                .map(tx -> toDTO(tx))
                .toList();
    }

    public TransactionDTO create(String email, TransactionDTO dto) {
        User user = getUser(email);

        Transaction tx = new Transaction();
        tx.setDescricao(dto.getDescricao());
        tx.setValor(dto.getValor());
        tx.setTipo(dto.getTipo());
        tx.setCategoria(dto.getCategoria());
        tx.setData(dto.getData());
        tx.setOrigem(dto.getOrigem());
        tx.setUser(user);

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
        tx.setOrigem(dto.getOrigem());

        return toDTO(transactionRepository.save(tx));
    }

    public void delete(String email, Long id) {
        transactionRepository.deleteById(id);
    }

    public Map<String, Object> getSummary(String email) {
        User user = getUser(email);

        Double receitas = transactionRepository.sumByUserIdAndTipo(user.getId(), "RECEITA");
        Double despesas = transactionRepository.sumByUserIdAndTipo(user.getId(), "DESPESA");

        receitas = receitas != null ? receitas : 0.0;
        despesas = despesas != null ? despesas : 0.0;

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