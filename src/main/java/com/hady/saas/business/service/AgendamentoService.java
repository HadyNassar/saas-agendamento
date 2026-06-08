package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.horario.HorarioFuncionamento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.HorarioFuncionamentoRepository;
import com.hady.saas.infrastructure.repository.ProfissionalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentoRepository;
    private final HorarioFuncionamentoRepository horarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ClienteService clienteService;
    private final NotificacaoService notificacaoService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              HorarioFuncionamentoRepository horarioRepository,
                              ProfissionalRepository profissionalRepository,
                              ClienteService clienteService,
                              @Lazy NotificacaoService notificacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.horarioRepository = horarioRepository;
        this.profissionalRepository = profissionalRepository;
        this.clienteService = clienteService;
        this.notificacaoService = notificacaoService;
    }

    // ── agendarPeloCliente agora aceita aguardandoPagamento ──────────────────
    // Quando aguardandoPagamento=true o status inicial é AGUARDANDO_PAGAMENTO,
    // e a confirmação por WhatsApp só é enviada após o pagamento ser aprovado.
    // Quando false, segue o fluxo normal (status AGENDADO + notificação imediata).
    @Transactional
    public Agendamento agendarPeloCliente(Cliente cliente, Profissional profissional,
                                          Servico servico, Estabelecimento estabelecimento,
                                          LocalDateTime inicio, boolean aguardandoPagamento) {

        if (cliente.isBloqueado()) {
            throw new RuntimeException("Cliente bloqueado: " + cliente.getMotivoBloqueio());
        }

        if (inicio.isBefore(LocalDateTime.now(ZONA_BRASIL))) {
            throw new RuntimeException("Não é possível agendar em uma data passada");
        }

        if (inicio.isAfter(LocalDateTime.now(ZONA_BRASIL).plusMonths(estabelecimento.getMesesAntecipacao()))) {
            throw new RuntimeException(
                    "Data excede o limite de antecipação de " + estabelecimento.getMesesAntecipacao() + " mês(es)");
        }

        if (servico.getDuracaoMinutos() == null || servico.getDuracaoMinutos() <= 0) {
            throw new RuntimeException("Duração do serviço inválida");
        }

        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        validarHorarioFuncionamento(estabelecimento.getId(), inicio, fim);
        validarConflito(profissional.getId(), inicio, fim, null);

        StatusAgendamento statusInicial = aguardandoPagamento
                ? StatusAgendamento.AGUARDANDO_PAGAMENTO
                : StatusAgendamento.AGENDADO;

        Agendamento agendamento = salvar(cliente, profissional, servico, estabelecimento,
                inicio, fim, true, statusInicial);

        // Só notifica se já está confirmado (não aguardando pagamento)
        if (!aguardandoPagamento) {
            try {
                notificacaoService.enviarConfirmacao(agendamento);
            } catch (Exception e) {
                log.error("[WhatsApp] Falha ao enviar confirmação para agendamento id={}: {}",
                        agendamento.getId(), e.getMessage(), e);
            }
        }

        return agendamento;
    }

    @Transactional
    public Agendamento agendarPeloPainel(Cliente cliente, Profissional profissional,
                                         Servico servico, Estabelecimento estabelecimento,
                                         LocalDateTime inicio) {

        if (inicio.isBefore(LocalDateTime.now(ZONA_BRASIL))) {
            throw new RuntimeException("Não é possível agendar em uma data passada");
        }

        if (inicio.isAfter(LocalDateTime.now(ZONA_BRASIL).plusMonths(estabelecimento.getMesesAntecipacao()))) {
            throw new RuntimeException(
                    "Data excede o limite de antecipação de " + estabelecimento.getMesesAntecipacao() + " mês(es)");
        }

        if (servico.getDuracaoMinutos() == null || servico.getDuracaoMinutos() <= 0) {
            throw new RuntimeException("Duração do serviço inválida");
        }

        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        validarHorarioFuncionamento(estabelecimento.getId(), inicio, fim);
        validarConflito(profissional.getId(), inicio, fim, null);

        Agendamento agendamento = salvar(cliente, profissional, servico, estabelecimento,
                inicio, fim, false, StatusAgendamento.AGENDADO);

        try {
            notificacaoService.enviarConfirmacao(agendamento);
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao enviar confirmação para agendamento id={}: {}",
                    agendamento.getId(), e.getMessage(), e);
        }

        return agendamento;
    }

    @Transactional
    public Agendamento cancelar(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RuntimeException("Apenas agendamentos AGENDADO podem ser cancelados");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        try {
            notificacaoService.enviarCancelamento(salvo);
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao enviar cancelamento para agendamento id={}: {}",
                    salvo.getId(), e.getMessage(), e);
        }

        return salvo;
    }

    @Transactional
    public Agendamento concluir(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RuntimeException("Apenas agendamentos AGENDADO podem ser concluidos");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento marcarFalta(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RuntimeException("Apenas agendamentos AGENDADO podem ser marcados como falta");
        }

        agendamento.setStatus(StatusAgendamento.FALTOU);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        clienteService.registrarFalta(agendamento.getCliente().getId(), estabelecimentoId);

        try {
            notificacaoService.enviarFalta(salvo);
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao enviar notificação de falta para agendamento id={}: {}",
                    salvo.getId(), e.getMessage(), e);
        }

        return salvo;
    }

    public void enviarLembrete(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);
        notificacaoService.enviarLembrete(agendamento);
    }

    public List<Agendamento> listarPorDia(Long estabelecimentoId, LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(23, 59, 59);
        return agendamentoRepository.findByEstabelecimentoIdAndDataHoraInicioBetween(
                estabelecimentoId, inicio, fim);
    }

    public List<Agendamento> listarPorProfissionalEDia(Long profissionalId, Long estabelecimentoId, LocalDate dia) {
        profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(23, 59, 59);
        return agendamentoRepository.findByProfissionalIdAndDataHoraInicioBetween(
                profissionalId, inicio, fim);
    }

    public List<LocalTime> gerarSlotsDisponiveis(Long profissionalId, Long estabelecimentoId,
                                                 LocalDate dia, int duracaoServico) {

        profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        if (duracaoServico <= 0) {
            throw new RuntimeException("Duração do serviço inválida");
        }

        String diaSemana = dia.getDayOfWeek().name();
        HorarioFuncionamento horario = horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, diaSemana)
                .orElseThrow(() -> new RuntimeException(
                        "Horario de funcionamento nao configurado para: " + diaSemana));

        if (!horario.isAberto()) {
            return new ArrayList<>();
        }

        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime fimDia = dia.atTime(23, 59, 59);
        List<Agendamento> agendamentosExistentes = agendamentoRepository
                .findByProfissionalIdAndDataHoraInicioBetween(profissionalId, inicioDia, fimDia);

        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = horario.getHoraAbertura();
        LocalTime fechamento = horario.getHoraFechamento();
        int intervalo = horario.getIntervaloMinutos();

        LocalDate hoje = LocalDate.now(ZONA_BRASIL);
        LocalTime agora = LocalTime.now(ZONA_BRASIL);

        while (cursor.plusMinutes(duracaoServico).compareTo(fechamento) <= 0) {

            if (dia.equals(hoje) && !cursor.isAfter(agora)) {
                cursor = cursor.plusMinutes(duracaoServico + intervalo);
                continue;
            }

            if (horario.getInicioAlmoco() != null && horario.getFimAlmoco() != null) {
                if (!cursor.isBefore(horario.getInicioAlmoco()) &&
                        cursor.isBefore(horario.getFimAlmoco())) {
                    cursor = horario.getFimAlmoco();
                    continue;
                }
            }

            LocalDateTime slotInicio = dia.atTime(cursor);
            LocalDateTime slotFim = slotInicio.plusMinutes(duracaoServico);

            boolean temConflito = agendamentosExistentes.stream()
                    .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO
                            || a.getStatus() == StatusAgendamento.AGUARDANDO_PAGAMENTO)
                    .anyMatch(a -> slotInicio.isBefore(a.getDataHoraFim()) &&
                            slotFim.isAfter(a.getDataHoraInicio()));

            if (!temConflito) {
                slots.add(cursor);
            }

            cursor = cursor.plusMinutes(duracaoServico + intervalo);
        }

        return slots;
    }

    public List<LocalTime> gerarTodosSlots(Long profissionalId, Long estabelecimentoId,
                                           LocalDate dia, int duracaoServico) {

        profissionalRepository.findByIdAndEstabelecimentoId(profissionalId, estabelecimentoId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        if (duracaoServico <= 0) {
            throw new RuntimeException("Duração do serviço inválida");
        }

        String diaSemana = dia.getDayOfWeek().name();

        HorarioFuncionamento horario = horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, diaSemana)
                .orElseThrow(() -> new RuntimeException(
                        "Horario de funcionamento nao configurado para: " + diaSemana));

        if (!horario.isAberto()) {
            return new ArrayList<>();
        }

        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = horario.getHoraAbertura();
        LocalTime fechamento = horario.getHoraFechamento();
        int intervalo = horario.getIntervaloMinutos();

        while (cursor.plusMinutes(duracaoServico).compareTo(fechamento) <= 0) {

            if (horario.getInicioAlmoco() != null && horario.getFimAlmoco() != null) {
                if (!cursor.isBefore(horario.getInicioAlmoco()) &&
                        cursor.isBefore(horario.getFimAlmoco())) {
                    cursor = horario.getFimAlmoco();
                    continue;
                }
            }

            slots.add(cursor);
            cursor = cursor.plusMinutes(duracaoServico + intervalo);
        }

        return slots;
    }

    public Agendamento buscarPorId(Long id, Long estabelecimentoId) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!agendamento.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Agendamento não pertence a este estabelecimento");
        }

        return agendamento;
    }

    // salvar agora recebe o status como parâmetro em vez de hardcodar AGENDADO
    private Agendamento salvar(Cliente cliente, Profissional profissional, Servico servico,
                               Estabelecimento estabelecimento, LocalDateTime inicio,
                               LocalDateTime fim, boolean agendadoPeloCliente,
                               StatusAgendamento status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setEstabelecimento(estabelecimento);
        agendamento.setDataHoraInicio(inicio);
        agendamento.setDataHoraFim(fim);
        agendamento.setAgendadoPeloCliente(agendadoPeloCliente);
        agendamento.setStatus(status);
        return agendamentoRepository.save(agendamento);
    }

    private void validarHorarioFuncionamento(Long estabelecimentoId,
                                             LocalDateTime inicio, LocalDateTime fim) {

        String diaSemana = inicio.getDayOfWeek().name();

        HorarioFuncionamento horario = horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, diaSemana)
                .orElseThrow(() -> new RuntimeException(
                        "Horario de funcionamento nao configurado para: " + diaSemana));

        if (!horario.isAberto()) {
            throw new RuntimeException("Estabelecimento fechado neste dia");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalDateTime fechamentoDateTime = inicio.toLocalDate().atTime(horario.getHoraFechamento());

        if (horaInicio.isBefore(horario.getHoraAbertura()) ||
                fim.isAfter(fechamentoDateTime)) {
            throw new RuntimeException("Horario fora do funcionamento do estabelecimento");
        }

        if (horario.getInicioAlmoco() != null && horario.getFimAlmoco() != null) {
            LocalTime horaFimLocal = inicio.toLocalTime().plusMinutes(
                    java.time.Duration.between(inicio, fim).toMinutes());
            if (horaInicio.isBefore(horario.getFimAlmoco()) &&
                    horaFimLocal.isAfter(horario.getInicioAlmoco())) {
                throw new RuntimeException("Horario conflita com o intervalo de almoco");
            }
        }
    }

    private void validarConflito(Long profissionalId, LocalDateTime inicio,
                                 LocalDateTime fim, Long agendamentoIdIgnorar) {

        List<Agendamento> conflitos = agendamentoRepository.findConflitos(profissionalId, inicio, fim);

        if (agendamentoIdIgnorar != null) {
            conflitos = conflitos.stream()
                    .filter(a -> !a.getId().equals(agendamentoIdIgnorar))
                    .toList();
        }

        if (!conflitos.isEmpty()) {
            throw new RuntimeException("Profissional ja possui agendamento neste horario");
        }
    }
}