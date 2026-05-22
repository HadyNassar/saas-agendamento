package com.hady.saas.business.service;

import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;

    public DashboardService(AgendamentoRepository agendamentoRepository,
                            ClienteRepository clienteRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.clienteRepository = clienteRepository;
    }

    // Retorna resumo completo do dashboard por período
    public Map<String, Object> gerarDashboard(Long estabelecimentoId, LocalDate dataInicio, LocalDate dataFim) {

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        var agendamentos = agendamentoRepository
                .findByEstabelecimentoIdAndDataHoraInicioBetween(estabelecimentoId, inicio, fim);

        // Total de atendimentos por status
        long totalAgendados = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO).count();
        long totalConcluidos = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO).count();
        long totalCancelados = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CANCELADO).count();
        long totalFaltas = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.FALTOU).count();
        long totalGeral = agendamentos.size();

        // Faturamento — soma dos preços dos serviços concluídos
        BigDecimal faturamento = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .map(a -> a.getServico().getPreco())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Taxa de faltas
        double taxaFaltas = totalGeral > 0
                ? (double) totalFaltas / totalGeral * 100
                : 0;

        // Serviços mais vendidos (concluídos)
        Map<String, Long> servicosMaisVendidos = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .collect(Collectors.groupingBy(
                        a -> a.getServico().getNome(),
                        Collectors.counting()
                ));

        // Ranking de profissionais por atendimentos concluídos
        Map<String, Long> rankingProfissionais = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .collect(Collectors.groupingBy(
                        a -> a.getProfissional().getNome(),
                        Collectors.counting()
                ));

        // Faturamento por profissional
        Map<String, BigDecimal> faturamentoPorProfissional = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .collect(Collectors.groupingBy(
                        a -> a.getProfissional().getNome(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> a.getServico().getPreco(),
                                BigDecimal::add)
                ));

        // Total de clientes do estabelecimento
        long totalClientes = clienteRepository.findByEstabelecimentoId(estabelecimentoId).size();

        // Monta o resultado
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("periodo", Map.of("inicio", dataInicio, "fim", dataFim));
        dashboard.put("atendimentos", Map.of(
                "total", totalGeral,
                "agendados", totalAgendados,
                "concluidos", totalConcluidos,
                "cancelados", totalCancelados,
                "faltas", totalFaltas
        ));
        dashboard.put("faturamento", faturamento);
        dashboard.put("taxaFaltas", String.format("%.1f%%", taxaFaltas));
        dashboard.put("servicosMaisVendidos", servicosMaisVendidos);
        dashboard.put("rankingProfissionais", rankingProfissionais);
        dashboard.put("faturamentoPorProfissional", faturamentoPorProfissional);
        dashboard.put("totalClientes", totalClientes);

        return dashboard;
    }
}
