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

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              HorarioFuncionamentoRepository horarioRepository,
                              ClienteService clienteService) {
        this.agendamentoRepository = agendamentoRepository;
        this.horarioRepository = horarioRepository;
        this.clienteService = clienteService;
    }

    // Cria agendamento pelo LINK PÚBLICO (cliente final)
    // Valida horário de funcionamento e conflitos
    public Agendamento agendarPeloCliente(Cliente cliente, Profissional profissional,
                                          Servico servico, Estabelecimento estabelecimento,
                                          LocalDateTime inicio) {

        // Verifica se cliente está bloqueado
        if (cliente.isBloqueado()) {
            throw new RuntimeException("Cliente bloqueado: " + cliente.getMotivoBloqueio());
        }

        // Calcula horário fim: início + duração do serviço
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());

        // Valida horário de funcionamento para agendamentos pelo cliente
        validarHorarioFuncionamento(estabelecimento.getId(), inicio, fim);

        // Verifica conflito de horário do profissional
        validarConflito(profissional.getId(), inicio, fim, null);

        return salvar(cliente, profissional, servico, estabelecimento, inicio, fim, true);
    }

    // Cria agendamento pelo PAINEL INTERNO (ADMIN ou FUNCIONARIO)
    // Não valida horário de funcionamento — pode agendar em qualquer horário
    public Agendamento agendarPeloPainel(Cliente cliente, Profissional profissional,
                                         Servico servico, Estabelecimento estabelecimento,
                                         LocalDateTime inicio) {

        // Calcula horário fim: início + duração do serviço
        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());

        // Verifica apenas conflito de horário — sem restrição de funcionamento
        validarConflito(profissional.getId(), inicio, fim, null);

        return salvar(cliente, profissional, servico, estabelecimento, inicio, fim, false);
    }

    // Cancela agendamento
    public Agendamento cancelar(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RuntimeException("Apenas agendamentos com status AGENDADO podem ser cancelados");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return agendamentoRepository.save(agendamento);
    }

    // Conclui atendimento
    public Agendamento concluir(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RuntimeException("Apenas agendamentos com status AGENDADO podem ser concluídos");
        }

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return agendamentoRepository.save(agendamento);
    }

    // Marca cliente como FALTOU e incrementa contador de faltas
    public Agendamento marcarFalta(Long agendamentoId, Long estabelecimentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId, estabelecimentoId);

        agendamento.setStatus(StatusAgendamento.FALTOU);
        agendamentoRepository.save(agendamento);

        // Incrementa contador de faltas do cliente
        clienteService.registrarFalta(agendamento.getCliente().getId(), estabelecimentoId);

        return agendamento;
    }

    // Lista agendamentos do estabelecimento por dia
    public List<Agendamento> listarPorDia(Long estabelecimentoId, LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(23, 59, 59);
        return agendamentoRepository.findByEstabelecimentoIdAndDataHoraInicioBetween(
                estabelecimentoId, inicio, fim);
    }

    // Lista agendamentos de um profissional por dia
    public List<Agendamento> listarPorProfissionalEDia(Long profissionalId, LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.atTime(23, 59, 59);
        return agendamentoRepository.findByProfissionalIdAndDataHoraInicioBetween(
                profissionalId, inicio, fim);
    }

    // Gera slots de horários disponíveis para um profissional em um dia
    // Usado pelo link público para mostrar horários ao cliente
    public List<LocalTime> gerarSlotsDisponiveis(Long profissionalId, Long estabelecimentoId,
                                                  LocalDate dia, int duracaoServico) {

        // Busca horário de funcionamento do dia
        HorarioFuncionamento horario = horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, dia.getDayOfWeek())
                .orElseThrow(() -> new RuntimeException("Horário de funcionamento não configurado para este dia"));

        if (!horario.isAberto()) {
            return new ArrayList<>(); // estabelecimento fechado neste dia
        }

        // Busca agendamentos já existentes do profissional neste dia
        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime fimDia = dia.atTime(23, 59, 59);
        List<Agendamento> agendamentosExistentes = agendamentoRepository
                .findByProfissionalIdAndDataHoraInicioBetween(profissionalId, inicioDia, fimDia);

        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = horario.getHoraAbertura();
        LocalTime fechamento = horario.getHoraFechamento();
        int intervalo = horario.getIntervaloMinutos();

        while (cursor.plusMinutes(duracaoServico).compareTo(fechamento) <= 0) {

            // Pula horário de almoço
            if (horario.getInicioAlmoco() != null && horario.getFimAlmoco() != null) {
                if (!cursor.isBefore(horario.getInicioAlmoco()) &&
                    cursor.isBefore(horario.getFimAlmoco())) {
                    cursor = horario.getFimAlmoco();
                    continue;
                }
            }

            LocalDateTime slotInicio = dia.atTime(cursor);
            LocalDateTime slotFim = slotInicio.plusMinutes(duracaoServico);

            // Verifica se o slot conflita com algum agendamento existente
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
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        if (!agendamento.getEstabelecimento().getId().equals(estabelecimentoId)) {
            throw new RuntimeException("Agendamento não pertence a este estabelecimento");
        }

        return agendamento;
    }

    // ==================== MÉTODOS PRIVADOS ====================

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

    // Valida se horário está dentro do funcionamento do estabelecimento
    private void validarHorarioFuncionamento(Long estabelecimentoId,
                                              LocalDateTime inicio, LocalDateTime fim) {

        HorarioFuncionamento horario = horarioRepository
                .findByEstabelecimentoIdAndDiaSemana(estabelecimentoId, inicio.getDayOfWeek())
                .orElseThrow(() -> new RuntimeException("Horário de funcionamento não configurado para este dia"));

        if (!horario.isAberto()) {
            throw new RuntimeException("Estabelecimento fechado neste dia");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFim = fim.toLocalTime();

        if (horaInicio.isBefore(horario.getHoraAbertura()) ||
            horaFim.isAfter(horario.getHoraFechamento())) {
            throw new RuntimeException("Horário fora do funcionamento do estabelecimento");
        }

        // Verifica conflito com horário de almoço
        if (horario.getInicioAlmoco() != null && horario.getFimAlmoco() != null) {
            if (horaInicio.isBefore(horario.getFimAlmoco()) &&
                horaFim.isAfter(horario.getInicioAlmoco())) {
                throw new RuntimeException("Horário conflita com o intervalo de almoço");
            }
        }
    }

    // Valida conflito de horário do profissional
    private void validarConflito(Long profissionalId, LocalDateTime inicio,
                                  LocalDateTime fim, Long agendamentoIdIgnorar) {

        List<Agendamento> conflitos = agendamentoRepository.findConflitos(profissionalId, inicio, fim);

        // Remove o próprio agendamento da validação (útil para edição futura)
        if (agendamentoIdIgnorar != null) {
            conflitos = conflitos.stream()
                    .filter(a -> !a.getId().equals(agendamentoIdIgnorar))
                    .toList();
        }

        if (!conflitos.isEmpty()) {
            throw new RuntimeException("Profissional já possui agendamento neste horário");
        }
    }
}
