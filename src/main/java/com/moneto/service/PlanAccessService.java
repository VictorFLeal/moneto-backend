package com.moneto.service;

import com.moneto.entity.User;
import com.moneto.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PlanAccessService {

    private final TransactionRepository transactionRepository;

    public PlanAccessService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public boolean isStart(User user) {
        return user.getPlano() == null || "start".equalsIgnoreCase(user.getPlano());
    }

    public boolean isEssencialOrHigher(User user) {
        if (user.getPlano() == null) return false;

        String plano = user.getPlano().toLowerCase();

        return plano.equals("essencial")
                || plano.equals("pro")
                || plano.equals("business");
    }

    public boolean isProOrBusiness(User user) {
        if (user.getPlano() == null) return false;

        String plano = user.getPlano().toLowerCase();

        return plano.equals("pro")
                || plano.equals("business");
    }

    public boolean isBusiness(User user) {
        return user.getPlano() != null && "business".equalsIgnoreCase(user.getPlano());
    }

    public void validarLimiteLancamentosStart(User user) {
        if (!isStart(user)) {
            return;
        }

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        long totalLancamentos = transactionRepository.countByUserIdAndDataBetween(
                user.getId(),
                inicioMes,
                fimMes
        );

        if (totalLancamentos >= 30) {
            throw new RuntimeException("Seu plano Start permite até 30 lançamentos por mês.");
        }
    }

    public boolean atingiuLimiteWhatsappStart(User user) {
        if (!isStart(user)) {
            return false;
        }

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        long whatsappUsado = transactionRepository.countByUserIdAndOrigemAndDataBetween(
                user.getId(),
                "whatsapp",
                inicioMes,
                fimMes
        );

        return whatsappUsado >= 10;
    }

    public void validarIA(User user) {
        if (!isProOrBusiness(user)) {
            throw new RuntimeException("A IA do MONETO está disponível apenas no plano Pro ou Business.");
        }
    }

    public void validarRelatoriosBasicos(User user) {
        if (!isEssencialOrHigher(user)) {
            throw new RuntimeException("Relatórios estão disponíveis apenas no plano Essencial ou superior.");
        }
    }

    public void validarRelatoriosAvancados(User user) {
        if (!isProOrBusiness(user)) {
            throw new RuntimeException("Relatórios avançados estão disponíveis apenas no plano Pro ou Business.");
        }
    }

    public void validarModoDividas(User user) {
        if (!isProOrBusiness(user)) {
            throw new RuntimeException("Modo sair das dívidas está disponível apenas no plano Pro ou Business.");
        }
    }

    public void validarModoEmpresa(User user) {
        if (!isBusiness(user)) {
            throw new RuntimeException("Modo empresa está disponível apenas no plano Business.");
        }
    }
}