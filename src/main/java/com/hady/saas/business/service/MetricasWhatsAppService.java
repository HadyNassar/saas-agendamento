package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.whatsapp.WhatsAppMensagem;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.ClienteRepository;
import com.hady.saas.infrastructure.repository.WhatsAppMensagemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Item 12 (camada ELITE/PLATINUM) — Métricas de WhatsApp no painel.
 *
 * Gating: disponível para qualquer estabelecimento com plano != STANDARD
 * (ELITE e PLATINUM são idênticos em funcionalidade — a única diferença
 * entre eles é limite de contas de usuário, não funcionalidade de robô ou
 * marketing). O controller é quem valida o plano antes de chamar este
 * service.
 *
 * Quatro métricas, cada uma com uma definição explícita porque nenhuma delas
 * tem um vínculo de dado direto e inequívoco no modelo atual — todas são
 * heurísticas sobre dados já existentes, DECISÕES DE PRODUTO confirmadas com
 * o dono nesta sessão (não são cálculos "óbvios" a partir do schema):
 *
 *   1. Taxa de confirmação: % de agendamentos com status AGENDADO no período
 *      (por dataHoraInicio) que têm confirmadoPeloCliente = true.
 *
 *   2. Agendamentos via campanha vs orgânico: um agendamento CRIADO (por
 *      criadoEm, não dataHoraInicio) no período conta como "via campanha" se
 *      o mesmo cliente recebeu ao menos uma WhatsAppMensagem tipo=CAMPANHA,
 *      status=ENVIADO, nos JANELA_CAMPANHA_DIAS dias imediatamente anteriores
 *      à criação do agendamento. Todo o resto conta como "orgânico". É uma
 *      heurística por correlação temporal, não um vínculo direto no banco —
 *      não existe hoje (e não foi pedido) um campo explícito de rastreamento
 *      de origem em Agendamento.
 *
 *   3. Retorno via lembrete de agendamento (24h/2h): dos agendamentos com
 *      dataHoraInicio no período, qual fração concluiu (CONCLUIDO) vs não
 *      compareceu (FALTOU) — proxy da eficácia do lembrete pré-atendimento
 *      já enviado por WhatsAppService.
 *
 *   4. Retorno via lembrete pós-atendimento: dos clientes que receberam o
 *      lembrete de retorno (Cliente.ultimoLembreteRetorno) dentro do
 *      período, quantos criaram um novo agendamento depois do envio do
 *      lembrete — mede se o lembrete de "já faz tempo que você não vem" de
 *      fato trouxe o cliente de volta.
 */
@Service
public class MetricasWhatsAppService {

    // Janela em dias após um envio de campanha dentro da qual um agendamento
    // novo do mesmo cliente ainda é atribuído a essa campanha. Decisão de
    // produto confirmada nesta sessão (27/07/2026) — não é um valor técnico
    // óbvio, ajustar aqui se o critério de negócio mudar.
    private static final int JANELA_CAMPANHA_DIAS = 7;

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final WhatsAppMensagemRepository whatsAppMensagemRepository;

    public MetricasWhatsAppService(AgendamentoRepository agendamentoRepository,
                                   ClienteRepository clienteRepository,
                                   WhatsAppMensagemRepository whatsAppMensagemRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.clienteRepository = clienteRepository;
        this.whatsAppMensagemRepository = whatsAppMensagemRepository;
    }

    public Map<String, Object> gerarMetricas(Long estabelecimentoId, LocalDate dataInicio, LocalDate dataFim) {

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("periodoInicio", dataInicio);
        resultado.put("periodoFim", dataFim);
        resultado.put("taxaConfirmacao", calcularTaxaConfirmacao(estabelecimentoId, inicio, fim));
        resultado.put("campanhaVsOrganico", calcularCampanhaVsOrganico(estabelecimentoId, inicio, fim));
        resultado.put("lembreteAgendamento", calcularLembreteAgendamento(estabelecimentoId, inicio, fim));
        resultado.put("lembreteRetorno", calcularLembreteRetorno(estabelecimentoId, inicio, fim));
        return resultado;
    }

    // ── 1. Taxa de confirmação ──────────────────────────────────────────────
    private Map<String, Object> calcularTaxaConfirmacao(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim) {
        List<Agendamento> agendamentos =
                agendamentoRepository.findPorEstabelecimentoEPeriodoAtendimento(estabelecimentoId, inicio, fim);

        long total = agendamentos.size();
        long confirmados = agendamentos.stream().filter(Agendamento::isConfirmadoPeloCliente).count();

        Map<String, Object> out = new HashMap<>();
        out.put("totalAgendamentos", total);
        out.put("totalConfirmados", confirmados);
        out.put("taxa", percentual(confirmados, total));
        return out;
    }

    // ── 2. Via campanha vs orgânico ─────────────────────────────────────────
    private Map<String, Object> calcularCampanhaVsOrganico(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim) {

        // Busca campanhas numa janela ampliada: um agendamento no início do
        // período do relatório pode ter sido motivado por uma campanha
        // enviada até JANELA_CAMPANHA_DIAS dias ANTES do início do período.
        LocalDateTime desdeCampanhas = inicio.minusDays(JANELA_CAMPANHA_DIAS);
        List<WhatsAppMensagem> campanhas =
                whatsAppMensagemRepository.findCampanhasEnviadasParaMetricas(estabelecimentoId, desdeCampanhas, fim);

        // Agrupa os envios de campanha por cliente para checar a janela por
        // agendamento sem N+1 (uma lista ordenada de instantes de envio por
        // cliente já é suficiente, não precisa reprocessar por campanha).
        Map<Long, List<LocalDateTime>> enviosPorCliente = campanhas.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCliente().getId(),
                        Collectors.mapping(WhatsAppMensagem::getEnviadoEm, Collectors.toList())));

        List<Agendamento> criados =
                agendamentoRepository.findCriadosEntrePorEstabelecimento(estabelecimentoId, inicio, fim);

        long viaCampanha = 0;
        long organico = 0;

        for (Agendamento a : criados) {
            Long clienteId = a.getCliente().getId();
            List<LocalDateTime> envios = enviosPorCliente.get(clienteId);
            boolean atribuidoACampanha = false;

            if (envios != null) {
                LocalDateTime criadoEm = a.getCriadoEm();
                for (LocalDateTime envio : envios) {
                    if (!envio.isAfter(criadoEm) && !envio.isBefore(criadoEm.minusDays(JANELA_CAMPANHA_DIAS))) {
                        atribuidoACampanha = true;
                        break;
                    }
                }
            }

            if (atribuidoACampanha) {
                viaCampanha++;
            } else {
                organico++;
            }
        }

        long total = viaCampanha + organico;

        Map<String, Object> out = new HashMap<>();
        out.put("totalAgendamentosCriados", total);
        out.put("viaCampanha", viaCampanha);
        out.put("organico", organico);
        out.put("percentualViaCampanha", percentual(viaCampanha, total));
        out.put("janelaDias", JANELA_CAMPANHA_DIAS);
        return out;
    }

    // ── 3. Retorno via lembrete de agendamento (24h/2h) ─────────────────────
    private Map<String, Object> calcularLembreteAgendamento(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim) {
        List<Agendamento> agendamentos =
                agendamentoRepository.findPorEstabelecimentoEPeriodoAtendimento(estabelecimentoId, inicio, fim);

        long concluidos = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO).count();
        long faltas = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.FALTOU).count();
        long baseComparecimento = concluidos + faltas;

        Map<String, Object> out = new HashMap<>();
        out.put("totalConcluidos", concluidos);
        out.put("totalFaltas", faltas);
        out.put("taxaComparecimento", percentual(concluidos, baseComparecimento));
        return out;
    }

    // ── 4. Retorno via lembrete pós-atendimento ─────────────────────────────
    private Map<String, Object> calcularLembreteRetorno(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim) {
        List<Cliente> clientesLembrados =
                clienteRepository.findComLembreteRetornoEntre(estabelecimentoId, inicio, fim);

        long totalLembrados = clientesLembrados.size();
        long retornaram = 0;

        for (Cliente c : clientesLembrados) {
            LocalDateTime dataLembrete = c.getUltimoLembreteRetorno();
            List<Agendamento> historico = agendamentoRepository
                    .findTop20ByClienteIdOrderByDataHoraInicioDesc(c.getId(), estabelecimentoId);

            boolean voltou = historico.stream()
                    .anyMatch(a -> a.getCriadoEm() != null && a.getCriadoEm().isAfter(dataLembrete));

            if (voltou) retornaram++;
        }

        Map<String, Object> out = new HashMap<>();
        out.put("totalClientesLembrados", totalLembrados);
        out.put("totalRetornaram", retornaram);
        out.put("taxaRetorno", percentual(retornaram, totalLembrados));
        return out;
    }

    private double percentual(long parte, long total) {
        if (total == 0) return 0.0;
        return Math.round((parte * 10000.0) / total) / 100.0;
    }
}