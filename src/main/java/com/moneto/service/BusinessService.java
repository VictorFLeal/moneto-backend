package com.moneto.service;

import com.moneto.dto.BusinessEntryDTO;
import com.moneto.entity.BusinessEntry;
import com.moneto.entity.User;
import com.moneto.repository.BusinessEntryRepository;
import com.moneto.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BusinessService {

    private final BusinessEntryRepository businessEntryRepository;
    private final UserRepository userRepository;

    public BusinessService(BusinessEntryRepository businessEntryRepository, UserRepository userRepository) {
        this.businessEntryRepository = businessEntryRepository;
        this.userRepository = userRepository;
    }

    public List<BusinessEntryDTO> findAll(String email) {
        User user = getUser(email);

        return businessEntryRepository.findByUserIdOrderByDataDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public BusinessEntryDTO create(String email, BusinessEntryDTO dto) {
        User user = getUser(email);

        BusinessEntry entry = new BusinessEntry();
        entry.setDescricao(dto.getDescricao());
        entry.setTipo(dto.getTipo());
        entry.setCategoria(dto.getCategoria());
        entry.setValor(dto.getValor());
        entry.setData(dto.getData());
        entry.setVencimento(dto.getVencimento());
        entry.setStatus(dto.getStatus());
        entry.setUser(user);

        return toDTO(businessEntryRepository.save(entry));
    }

    public BusinessEntryDTO update(String email, Long id, BusinessEntryDTO dto) {
        BusinessEntry entry = businessEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro empresarial não encontrado"));

        entry.setDescricao(dto.getDescricao());
        entry.setTipo(dto.getTipo());
        entry.setCategoria(dto.getCategoria());
        entry.setValor(dto.getValor());
        entry.setData(dto.getData());
        entry.setVencimento(dto.getVencimento());
        entry.setStatus(dto.getStatus());

        return toDTO(businessEntryRepository.save(entry));
    }

    public void delete(String email, Long id) {
        businessEntryRepository.deleteById(id);
    }

    public Map<String, Object> summary(String email) {
        User user = getUser(email);

        List<BusinessEntry> entries = businessEntryRepository.findByUserIdOrderByDataDesc(user.getId());

        BigDecimal receitas = BigDecimal.ZERO;
        BigDecimal despesas = BigDecimal.ZERO;
        BigDecimal impostos = BigDecimal.ZERO;

        for (BusinessEntry e : entries) {
            BigDecimal valor = e.getValor() != null ? e.getValor() : BigDecimal.ZERO;

            if ("RECEITA".equalsIgnoreCase(e.getTipo())) {
                receitas = receitas.add(valor);
            } else if ("IMPOSTO".equalsIgnoreCase(e.getTipo())) {
                impostos = impostos.add(valor);
                despesas = despesas.add(valor);
            } else {
                despesas = despesas.add(valor);
            }
        }

        BigDecimal lucro = receitas.subtract(despesas);

        Map<String, Object> map = new HashMap<>();
        map.put("receitas", receitas);
        map.put("despesas", despesas);
        map.put("impostos", impostos);
        map.put("lucro", lucro);
        map.put("caixa", lucro);
        map.put("totalRegistros", entries.size());

        return map;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
    }

    private BusinessEntryDTO toDTO(BusinessEntry entry) {
        BusinessEntryDTO dto = new BusinessEntryDTO();

        dto.setId(entry.getId());
        dto.setDescricao(entry.getDescricao());
        dto.setTipo(entry.getTipo());
        dto.setCategoria(entry.getCategoria());
        dto.setValor(entry.getValor());
        dto.setData(entry.getData());
        dto.setVencimento(entry.getVencimento());
        dto.setStatus(entry.getStatus());

        return dto;
    }
}