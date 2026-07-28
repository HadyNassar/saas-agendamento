package com.hady.saas.business.service;

import com.hady.saas.application.dto.RelatorioFinanceiroDTO;
import com.hady.saas.application.dto.RelatorioDiaDTO;
import com.hady.saas.application.dto.RelatorioProfissionalDTO;
import com.hady.saas.application.dto.RelatorioResumoDTO;
import com.hady.saas.application.dto.RelatorioServicoDTO;
import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.Plano;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioFinanceiroService {

    private final AgendamentoRepository agendamentoRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public RelatorioFinanceiroService(AgendamentoRepository agendamentoRepository,
                                      EstabelecimentoRepository estabelecimentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    public RelatorioFinanceiroDTO gerarRelatorio(Long estabelecimentoId,
                                                 LocalDate inicio,
                                                 LocalDate fim) {

        Estabelecimento estab = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado."));

        if (estab.getPlanoEfetivo() == Plano.STANDARD) {
            throw new RuntimeException("Relatório financeiro disponível nos planos Elite e Platinum.");
        }

        LocalDateTime inicioDT = inicio.atStartOfDay();
        LocalDateTime fimDT    = fim.atTime(LocalTime.MAX);

        List<Agendamento> agendamentos = agendamentoRepository
                .findByEstabelecimentoIdAndStatusAndDataHoraInicioBetween(
                        estabelecimentoId,
                        StatusAgendamento.CONCLUIDO,
                        inicioDT,
                        fimDT
                );

        // ── Por serviço ──────────────────────────────────────────────────────
        Map<String, RelatorioServicoDTO> porServico = new LinkedHashMap<>();
        for (Agendamento ag : agendamentos) {
            String nomeServico  = ag.getServico().getNome();
            double preco        = precoLiquido(ag); // CORREÇÃO: usa valor com desconto, quando houver
            double custoFixo    = toDouble(ag.getServico().getCustoMaterialFixo());
            double custoPct     = toDouble(ag.getServico().getCustoMaterialPercent());
            double comissaoPct  = ag.getProfissional() != null
                    ? toDouble(ag.getProfissional().getComissaoPercent()) : 0;

            double custoMaterial = custoFixo + (preco * custoPct / 100);
            double comissao      = preco * comissaoPct / 100;

            porServico.computeIfAbsent(nomeServico, RelatorioServicoDTO::new)
                    .incrementar(preco, custoMaterial, comissao);
        }
        porServico.values().forEach(RelatorioServicoDTO::calcularMargem);

        // ── Por profissional ─────────────────────────────────────────────────
        Map<String, RelatorioProfissionalDTO> porProfissional = new LinkedHashMap<>();
        for (Agendamento ag : agendamentos) {
            if (ag.getProfissional() == null) continue;
            String nomeProfissional = ag.getProfissional().getNome();
            double preco       = precoLiquido(ag); // CORREÇÃO: usa valor com desconto, quando houver
            double custoFixo   = toDouble(ag.getServico().getCustoMaterialFixo());
            double custoPct    = toDouble(ag.getServico().getCustoMaterialPercent());
            double comissaoPct = toDouble(ag.getProfissional().getComissaoPercent());

            double custoMaterial = custoFixo + (preco * custoPct / 100);
            double comissao      = preco * comissaoPct / 100;

            porProfissional.computeIfAbsent(nomeProfissional, RelatorioProfissionalDTO::new)
                    .incrementar(preco, custoMaterial, comissao);
        }
        porProfissional.values().forEach(RelatorioProfissionalDTO::calcularMargem);

        // ── Por dia ──────────────────────────────────────────────────────────
        Map<LocalDate, double[]> porDiaMap = new TreeMap<>();
        for (Agendamento ag : agendamentos) {
            LocalDate data     = ag.getDataHoraInicio().toLocalDate();
            double preco       = precoLiquido(ag); // CORREÇÃO: usa valor com desconto, quando houver
            double custoFixo   = toDouble(ag.getServico().getCustoMaterialFixo());
            double custoPct    = toDouble(ag.getServico().getCustoMaterialPercent());
            double comissaoPct = ag.getProfissional() != null
                    ? toDouble(ag.getProfissional().getComissaoPercent()) : 0;

            double custoMaterial = custoFixo + (preco * custoPct / 100);
            double comissao      = preco * comissaoPct / 100;
            double lucro         = preco - custoMaterial - comissao;

            double[] vals = porDiaMap.computeIfAbsent(data, k -> new double[]{0, 0});
            vals[0] += preco;
            vals[1] += lucro;
        }

        List<RelatorioDiaDTO> porDia = porDiaMap.entrySet().stream()
                .map(e -> new RelatorioDiaDTO(
                        e.getKey().toString(),
                        round(e.getValue()[0]),
                        round(e.getValue()[1])))
                .collect(Collectors.toList());

        // ── Resumo ───────────────────────────────────────────────────────────
        double faturamentoBruto  = porServico.values().stream().mapToDouble(RelatorioServicoDTO::getFaturamento).sum();
        double totalComissoes    = porServico.values().stream().mapToDouble(RelatorioServicoDTO::getComissoes).sum();
        double totalCustoMat     = porServico.values().stream().mapToDouble(RelatorioServicoDTO::getCustoMaterial).sum();
        double totalCustos       = totalComissoes + totalCustoMat;
        double lucroLiquido      = faturamentoBruto - totalCustos;
        double margemLucro       = faturamentoBruto > 0 ? (lucroLiquido / faturamentoBruto) * 100 : 0;
        int    totalAtendimentos = agendamentos.size();
        double ticketMedio       = totalAtendimentos > 0 ? faturamentoBruto / totalAtendimentos : 0;

        RelatorioResumoDTO resumo = new RelatorioResumoDTO(
                round(faturamentoBruto), round(totalCustos), round(lucroLiquido),
                round(margemLucro), round(ticketMedio), totalAtendimentos,
                round(totalComissoes), round(totalCustoMat)
        );

        return new RelatorioFinanceiroDTO(
                resumo,
                porDia,
                new ArrayList<>(porServico.values()),
                new ArrayList<>(porProfissional.values())
        );
    }

    private double toDouble(BigDecimal v) {
        return v != null ? v.doubleValue() : 0.0;
    }

    // CORREÇÃO: preço do serviço menos o desconto de cupom aplicado ao
    // agendamento (quando houver) — mesma correção já aplicada em
    // PagamentoSplitService (sinal) e DashboardService (faturamento).
    //
    // ⚠️ DECISÃO IMPLÍCITA NESTA CORREÇÃO, NÃO VALIDADA COM O DONO DO
    // PRODUTO: como custoMaterial e comissão do profissional já eram
    // calculados como PERCENTUAL em cima da mesma variável `preco` usada
    // como faturamento, usar o valor líquido aqui também reduz a base de
    // cálculo da comissão do profissional quando há cupom (ex: comissão de
    // 20% sobre um serviço de R$100 com cupom de R$20 vira 20% de R$80, não
    // de R$100). Isso é defensável (comissão sobre o que foi efetivamente
    // recebido), mas é uma escolha, não uma regra confirmada — se o dono do
    // produto preferir que a comissão do profissional continue sobre o
    // preço cheio do serviço (cupom "sai do bolso da casa", não do
    // profissional), a base do cálculo de comissão precisaria ser separada
    // da base do faturamento reportado.
    private double precoLiquido(Agendamento agendamento) {
        BigDecimal preco = agendamento.getServico().getPreco();
        if (agendamento.getValorDescontoAplicado() != null) {
            preco = preco.subtract(agendamento.getValorDescontoAplicado()).max(BigDecimal.ZERO);
        }
        return toDouble(preco);
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}