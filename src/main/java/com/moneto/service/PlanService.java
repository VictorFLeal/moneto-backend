package com.moneto.service;

import com.moneto.dto.PlanDTO;
import com.moneto.entity.Plan;
import com.moneto.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PlanService implements CommandLineRunner {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        if (planRepository.count() > 0) return;

        planRepository.saveAll(List.of(
                criarPlano("start", 1, "MONETO Start", "R$ 0", "/ mês",
                        "Comece gratuitamente e sinta o valor", "Gratuito", "#4af0c4", false,
                        "Cadastro de receitas e despesas|Dashboard simples|Categorias padrão|Até 100 transações por mês",
                        "Sem IA|Sem WhatsApp|Sem relatórios avançados"),

                criarPlano("essencial", 2, "MONETO Essencial", "R$ 44,90", "/ mês",
                        "Organização financeira sem complicação", "Mais acessível", "#60a5fa", false,
                        "Receitas e despesas ilimitadas|Dashboard completo|Metas financeiras|Calendário financeiro|Notificações básicas",
                        "Sem IA avançada|Sem WhatsApp automático"),

                criarPlano("pro", 3, "MONETO Pro", "R$ 59,90", "/ mês",
                        "Controle financeiro inteligente com recursos avançados", "Principal", "#8b5cf6", true,
                        "Tudo do Essencial|Insights com IA|Relatórios avançados|Categorias personalizadas|Modo sair das dívidas|Alertas inteligentes",
                        "Sem módulo empresarial"),

                criarPlano("business", 4, "MONETO Business", "R$ 89,90", "/ mês",
                        "Gestão financeira para negócios e autônomos", "Empresas", "#f59e0b", false,
                        "Tudo do Pro|Controle por empresa|Relatórios de negócio|Visão de fluxo de caixa|Múltiplos usuários|Organização empresarial",
                        "Plano voltado para empresas")
        ));
    }

    private Plan criarPlano(
            String planId,
            Integer order,
            String nome,
            String preco,
            String periodo,
            String descricao,
            String tag,
            String cor,
            Boolean highlight,
            String features,
            String limits
    ) {
        Plan plan = new Plan();

        plan.setPlanId(planId);
        plan.setOrder(order);
        plan.setNome(nome);
        plan.setPreco(preco);
        plan.setPeriodo(periodo);
        plan.setDescricao(descricao);
        plan.setTag(tag);
        plan.setCor(cor);
        plan.setHighlight(highlight);
        plan.setFeatures(features);
        plan.setLimits(limits);
        plan.setAtivo(true);

        return plan;
    }

    public List<PlanDTO> findAll() {
        return planRepository.findByAtivoTrueOrderByOrderAsc()
                .stream()
                .map(plan -> toDTO(plan))
                .toList();
    }

    private PlanDTO toDTO(Plan plan) {
        PlanDTO dto = new PlanDTO();

        dto.setId(plan.getId());
        dto.setPlanId(plan.getPlanId());
        dto.setNome(plan.getNome());
        dto.setPreco(plan.getPreco());
        dto.setPeriodo(plan.getPeriodo());
        dto.setDescricao(plan.getDescricao());
        dto.setTag(plan.getTag());
        dto.setCor(plan.getCor());
        dto.setHighlight(plan.getHighlight());
        dto.setFeatures(splitText(plan.getFeatures()));
        dto.setLimits(splitText(plan.getLimits()));

        return dto;
    }

    private List<String> splitText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return Arrays.stream(text.split("\\|"))
                .map(String::trim)
                .toList();
    }
}