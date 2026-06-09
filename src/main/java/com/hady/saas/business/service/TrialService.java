package com.hady.saas.business.service;

import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrialService {

    private final EstabelecimentoRepository estabelecimentoRepo;

    public TrialService(EstabelecimentoRepository estabelecimentoRepo) {
        this.estabelecimentoRepo = estabelecimentoRepo;
    }

    @Transactional
    public Map<String, Object> iniciarTrial(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        // As validações (trialUsado e planoAtivo) ficam no domain method iniciarTrial()
        // para garantir consistência mesmo que o método seja chamado de outro lugar.
        estab.iniciarTrial();
        estabelecimentoRepo.save(estab);

        Map<String, Object> result = new HashMap<>();
        result.put("trialAtivo", true);
        result.put("trialInicio", estab.getTrialInicio());
        result.put("trialFim", estab.getTrialFim());
        result.put("diasRestantes", 5);
        // Durante o trial o usuário opera com acesso PLATINUM completo.
        result.put("planoEfetivo", estab.getPlanoEfetivo().name());
        result.put("limiteUsuarios", estab.getPlanoEfetivo().getLimiteUsuarios());
        return result;
    }

    public Map<String, Object> statusTrial(Long estabelecimentoId) {
        Estabelecimento estab = estabelecimentoRepo.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        Map<String, Object> result = new HashMap<>();
        result.put("trialUsado", estab.isTrialUsado());
        result.put("trialAtivo", estab.isTrialAtivo());
        result.put("trialFim", estab.getTrialFim());
        result.put("planoAtivo", estab.isPlanoAtivo());
        result.put("temAcesso", estab.temAcesso());
        // Plano efetivo: PLATINUM durante trial, plano real caso contrário.
        // O frontend usa esse campo para exibir limites e features corretos.
        result.put("planoEfetivo", estab.getPlanoEfetivo().name());
        result.put("limiteUsuarios", estab.getPlanoEfetivo().getLimiteUsuarios());

        if (estab.isTrialAtivo() && estab.getTrialFim() != null) {
            long diasRestantes = LocalDate.now().until(estab.getTrialFim()).getDays();
            result.put("diasRestantes", Math.max(0, diasRestantes));
        }

        return result;
    }
}
