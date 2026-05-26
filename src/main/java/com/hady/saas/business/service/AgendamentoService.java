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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final HorarioFuncionamentoRepository horarioRepository;
    private final ClienteService clienteService;
    private final NotificacaoService notificacaoService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              HorarioFuncionamentoRepository horarioRepository,
                              ClienteService clienteService,
                              @Lazy NotificacaoService notificacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.horarioRepository = horarioRepository;
        this.clienteService = clienteService;
        this.notificacaoService = notificacaoService;
    }

    public Agendamento agendarPeloCliente(Cliente cliente, Profissional profissional,
                                          Servico servico, Estabelecimento estabelecimento,
                                          LocalDateTime inicio) {

        if (cliente.isBloqueado()) {
            throw new RuntimeException("Cliente bloqueado: " + cliente.getMotivoBloqueio());
        }

        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        validarHorarioFuncionamento(estabelecimento.getId(), inicio, fim);
        validarConflito(profissional.getId(), inicio, fim, null);

        Agendamento agendamento = salvar(cliente, profissional, servico, estabelecimento, inicio, fim, true);

        // Notificação via WhatsApp (NotificacaoService já delega ao WhatsAppService)
        try {
            notificacaoService.enviarConfirmacao(agendamento);
        } catch (Exception e) {
            System.err.println("Erro ao enviar confirmação WhatsApp: " + e.getMessage());
        }

        return agendamento;
    }

    public Agendamento agendarPeloPainel(Cliente cliente, Profissional profissional,
                                         Servico servico, Estabelecimento estabelecimento,
                                         LocalDateTime inicio) {

        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        validarConflito(profissional.getId(), inicio, fim, null);

        Agendamento agendamento = salvar(cliente, profissional, servico, estabelecimento, inicio, fim, false);

        try {
            notificacaoService.enviarConfirmacao(agendamento);
        } catch (Exception e) {
            System.err.println("Erro ao enviar confirmação WhatsApp: " + e.getMessage());
        }

        return agendamento;
    }

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
            System.err.println("Erro ao enviar cancelamento WhatsApp: " + e.getMessage());
        }

        return salvo;
    }

    public Agendamento concluir(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RuntimeException("Apenas agendamentos AGENDADO podem ser concluidos");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return agendamentoRepository.save(agendamento);
    }

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
            System.err.println("Erro ao enviar notificação de falta WhatsApp: " + e.getMessage());
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

    public List<Agendamento> listarPorProfissionalEDia(Long profissionalId, LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(23, 59, 59);
        return agendamentoRepository.findByProfissionalIdAndDataHoraInicioBetween(
                profissionalId, inicio, fim);
    }

    public List<LocalTime> gerarSlotsDisponiveis(Long profissionalId, Long estabelecimentoId,
                                                 LocalDate dia, int duracaoServico) {

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

        while (cursor.plusMinutes(duracaoServico).compareTo(fechamento) <= 0) {

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
                    .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO)
                    .anyMatch(a -> slotInicio.isBefore(a.getDataHoraFim()) &&
                            slotFim.isAfter(a.getDataHoraInicio()));

            if (!temConflito) {
                slots.add(cursor);
            }

            cursor = cursor.plusMinutes(duracaoServico + intervalo);
        }

        return slots;
    }

    public Agendamento buscarPorId(Long id, Long estabelecimentoId) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento nao encontrado"));

        if (!agendamento.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Agendamento nao pertence a este estabelecimento");
        }

        return agendamento;
    }

    private Agendamento salvar(Cliente cliente, Profissional profissional, Servico servico,
                               Estabelecimento estabelecimento, LocalDateTime inicio,
                               LocalDateTime fim, boolean agendadoPeloCliente) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setEstabelecimento(estabelecimento);
        agendamento.setDataHoraInicio(inicio);
        agendamento.setDataHoraFim(fim);
        agendamento.setAgendadoPeloCliente(agendadoPeloCliente);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
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
        LocalTime horaFim = fim.toLocalTime();

        if (horaInicio.isBefore(horario.getHoraAbertura()) ||
                horaFim.isAfter(horario.getHoraFechamento())) {
            throw new RuntimeException("Horario fora do funcionamento do estabelecimento");
        }

        if (horario.getInicioAlmoco() != null && horario.getFimAlmoco() != null) {
            if (horaInicio.isBefore(horario.getFimAlmoco()) &&
                    horaFim.isAfter(horario.getInicioAlmoco())) {
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
