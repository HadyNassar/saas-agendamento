package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.ProfissionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item 13 (camada ELITE/PLATINUM) — Inteligência de agenda.
 *
 * Gating: != Plano.STANDARD (mesmo padrão do resto do robô/marketing — ELITE
 * e PLATINUM são idênticos em funcionalidade). O controller é quem valida o
 * plano antes de chamar este service.
 *
 * Reaproveita a fila de campanha já pronta (CampanhaEnvio/CampanhaEnvioItem +
 * CampanhaEnvioProcessorJob) — este service NÃO dispara nada sozinho. Ele só
 * DETECTA e relata; se o dono do salão quiser agir sobre um horário vazio
 * detectado, o fluxo é o mesmo de duas etapas da campanha manual (seção 5-B):
 * o dono vê o relatório no painel e, se quiser, cria uma campanha manual pelo
 * CampanhaEnvioController — não existe disparo automático aqui.
 *
 * Duas detecções, cada uma respondendo a uma pergunta diferente:
 *
 *   1. VAZIO FUTURO PRÓXIMO — granularidade de slot (30 minutos, unidade
 *      fixa de análise geral já que aqui não há um serviço específico sendo
 *      agendado). Para cada profissional ativo e cada um dos próximos N dias
 *      (N configurável via parâmetro, padrão 7 — decisão de produto
 *      confirmada em sessão), reaproveita
 *      AgendamentoService.gerarSlotsDisponiveis(...) com duração=30min para
 *      saber quais horários dentro do expediente estão livres. Também
 *      calcula, por dia, se o dia inteiro tem poucos agendamentos (menos de
 *      LIMITE_DIA_FRACO_ATIVOS agendamentos ativos) — o critério de "dia
 *      fraco" aqui é sobre o volume de agendamentos JÁ MARCADOS pro dia, não
 *      sobre slots livres (um dia pode ter poucos slots vazios mas ainda
 *      assim estar "fraco" se o expediente é curto, e vice-versa).
 *
 *   2. PADRÃO HISTÓRICO DE DIA FRACO — compara, nos últimos 30 dias
 *      corridos, o volume de agendamentos concluídos/agendados por
 *      dia-da-semana. Um dia-da-semana conta como "historicamente fraco" se
 *      seu volume médio de agendamentos por ocorrência está abaixo da média
 *      geral de todos os dias-da-semana no mesmo período (critério simples,
 *      decisão de produto confirmada em sessão — sem desvio padrão nem
 *      análise estatística mais sofisticada, que pode ser adicionada depois
 *      se o dono achar o critério simples insuficiente).
 */
@Service
public class InteligenciaAgendaService {

    private static final Logger log = LoggerFactory.getLogger(InteligenciaAgendaService.class);

    // Granularidade de slot usada pra varrer a agenda em busca de vazios,
    // já que aqui não existe um "serviço específico" sendo agendado (é uma
    // análise geral, não uma tela de marcação). Decisão de produto minha,
    // não validada — 30 min é o menor grão comum de serviço no sistema hoje,
    // ajustar se o dono quiser outro valor.
    private static final int SLOT_MINUTOS = 30;

    // Um dia com menos que este número de agendamentos ativos (AGENDADO ou
    // AGUARDANDO_PAGAMENTO) conta como "dia fraco" na detecção de vazio
    // futuro. Placeholder meu, não validado com o dono — ajustar conforme
    // feedback real de uso.
    private static final int LIMITE_DIA_FRACO_ATIVOS = 3;

    // Janela de análise do padrão histórico: últimos 30 dias corridos a
    // partir de hoje. Decisão de produto confirmada em sessão.
    private static final int JANELA_HISTORICO_DIAS = 30;

    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoService agendamentoService;

    public InteligenciaAgendaService(ProfissionalRepository profissionalRepository,
                                     AgendamentoRepository agendamentoRepository,
                                     AgendamentoService agendamentoService) {
        this.profissionalRepository = profissionalRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.agendamentoService = agendamentoService;
    }

    // ── 1. Vazio futuro próximo ─────────────────────────────────────────────
    public Map<String, Object> detectarVazioFuturo(Long estabelecimentoId, int diasAFrente) {

        List<Profissional> profissionais =
                profissionalRepository.findByEstabelecimentoIdAndAtivo(estabelecimentoId, true);

        LocalDate hoje = LocalDate.now();
        List<Map<String, Object>> porProfissional = new ArrayList<>();

        for (Profissional prof : profissionais) {
            List<Map<String, Object>> diasComVazio = new ArrayList<>();

            for (int i = 0; i < diasAFrente; i++) {
                LocalDate dia = hoje.plusDays(i);

                List<LocalTime> slotsLivres;
                try {
                    slotsLivres = agendamentoService.gerarSlotsDisponiveis(
                            prof.getId(), estabelecimentoId, dia, SLOT_MINUTOS);
                } catch (RuntimeException e) {
                    // Sem HorarioFuncionamento configurado para este dia da
                    // semana — não é um erro de negócio, só não há dado pra
                    // analisar esse dia. Pula silenciosamente.
                    log.debug("[InteligenciaAgenda] Sem horário configurado para {} ({}): {}",
                            dia, prof.getNome(), e.getMessage());
                    continue;
                }

                if (slotsLivres.isEmpty()) continue;

                long ativosNoDia = contarAgendamentosAtivosDoProfissionalNoDia(prof.getId(), dia);

                Map<String, Object> diaInfo = new HashMap<>();
                diaInfo.put("data", dia);
                diaInfo.put("totalSlotsLivres", slotsLivres.size());
                diaInfo.put("slotsLivres", slotsLivres);
                diaInfo.put("totalAgendamentosAtivos", ativosNoDia);
                diaInfo.put("diaFraco", ativosNoDia < LIMITE_DIA_FRACO_ATIVOS);
                diasComVazio.add(diaInfo);
            }

            if (!diasComVazio.isEmpty()) {
                Map<String, Object> profInfo = new HashMap<>();
                profInfo.put("profissionalId", prof.getId());
                profInfo.put("profissionalNome", prof.getNome());
                profInfo.put("dias", diasComVazio);
                porProfissional.add(profInfo);
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("diasAnalisados", diasAFrente);
        out.put("slotMinutos", SLOT_MINUTOS);
        out.put("limiteDiaFracoAtivos", LIMITE_DIA_FRACO_ATIVOS);
        out.put("profissionais", porProfissional);
        return out;
    }

    private long contarAgendamentosAtivosDoProfissionalNoDia(Long profissionalId, LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(23, 59, 59);

        return agendamentoRepository
                .findByProfissionalIdAndDataHoraInicioBetween(profissionalId, inicio, fim)
                .stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO
                        || a.getStatus() == StatusAgendamento.AGUARDANDO_PAGAMENTO)
                .count();
    }

    // ── 2. Padrão histórico de dia fraco ────────────────────────────────────
    public Map<String, Object> detectarPadraoHistorico(Long estabelecimentoId) {

        LocalDate hoje = LocalDate.now();
        LocalDate inicioJanela = hoje.minusDays(JANELA_HISTORICO_DIAS);

        List<Agendamento> agendamentos = agendamentoRepository.findByEstabelecimentoIdAndDataHoraInicioBetween(
                estabelecimentoId, inicioJanela.atStartOfDay(), hoje.atTime(23, 59, 59));

        // Conta agendamentos e ocorrências de cada dia-da-semana na janela,
        // pra chegar numa MÉDIA por ocorrência (não um total bruto, que
        // enviesaria dias-da-semana que aparecem mais vezes na janela).
        Map<DayOfWeek, Long> totalPorDiaSemana = new HashMap<>();
        Map<DayOfWeek, Long> ocorrenciasPorDiaSemana = new HashMap<>();

        for (DayOfWeek d : DayOfWeek.values()) {
            totalPorDiaSemana.put(d, 0L);
            ocorrenciasPorDiaSemana.put(d, contarOcorrenciasDoDiaSemana(inicioJanela, hoje, d));
        }

        for (Agendamento a : agendamentos) {
            DayOfWeek d = a.getDataHoraInicio().getDayOfWeek();
            totalPorDiaSemana.merge(d, 1L, Long::sum);
        }

        Map<DayOfWeek, Double> mediaPorDiaSemana = new HashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            long ocorrencias = ocorrenciasPorDiaSemana.get(d);
            double media = ocorrencias == 0 ? 0.0 : totalPorDiaSemana.get(d) / (double) ocorrencias;
            mediaPorDiaSemana.put(d, media);
        }

        double mediaGeral = mediaPorDiaSemana.values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            double media = mediaPorDiaSemana.get(d);
            Map<String, Object> item = new HashMap<>();
            item.put("diaSemana", d.name());
            item.put("mediaAgendamentosPorOcorrencia", Math.round(media * 100.0) / 100.0);
            item.put("fracoComparadoAMediaGeral", media < mediaGeral);
            resultado.add(item);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("janelaDias", JANELA_HISTORICO_DIAS);
        out.put("mediaGeralPorOcorrencia", Math.round(mediaGeral * 100.0) / 100.0);
        out.put("porDiaSemana", resultado);
        return out;
    }

    private long contarOcorrenciasDoDiaSemana(LocalDate inicio, LocalDate fim, DayOfWeek diaSemana) {
        long count = 0;
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            if (cursor.getDayOfWeek() == diaSemana) count++;
            cursor = cursor.plusDays(1);
        }
        return count;
    }
}