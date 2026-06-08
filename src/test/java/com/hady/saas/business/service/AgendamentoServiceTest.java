package com.hady.saas.business.service;

import com.hady.saas.domain.agendamento.Agendamento;
import com.hady.saas.domain.cliente.Cliente;
import com.hady.saas.domain.enums.StatusAgendamento;
import com.hady.saas.domain.estabelecimento.Estabelecimento;
import com.hady.saas.domain.profissional.Profissional;
import com.hady.saas.domain.servico.Servico;
import com.hady.saas.domain.horario.HorarioFuncionamento;
import com.hady.saas.infrastructure.repository.AgendamentoRepository;
import com.hady.saas.infrastructure.repository.HorarioFuncionamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock AgendamentoRepository agendamentoRepository;
    @Mock HorarioFuncionamentoRepository horarioRepository;
    @Mock ClienteService clienteService;
    @Mock NotificacaoService notificacaoService;

    @InjectMocks AgendamentoService service;

    private Cliente cliente;
    private Profissional profissional;
    private Servico servico;
    private Estabelecimento estabelecimento;
    private HorarioFuncionamento horario;

    @BeforeEach
    void setUp() {
        estabelecimento = new Estabelecimento();
        estabelecimento.setId(1L);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setTelefone("67999999999");
        cliente.setBloqueado(false);

        profissional = new Profissional();
        profissional.setId(1L);
        profissional.setNome("Profissional Teste");

        servico = new Servico();
        servico.setId(1L);
        servico.setNome("Corte");
        servico.setDuracaoMinutos(60);
        servico.setPreco(new BigDecimal("50.00"));

        // Horário: seg-sex 08h-18h, sem almoço
        horario = new HorarioFuncionamento();
        horario.setAberto(true);
        horario.setHoraAbertura(LocalTime.of(8, 0));
        horario.setHoraFechamento(LocalTime.of(18, 0));
        horario.setIntervaloMinutos(0);
    }

    // ── agendarPeloCliente ────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve lançar exceção quando cliente está bloqueado")
    void agendarPeloCliente_clienteBloqueado_lancaExcecao() {
        cliente.setBloqueado(true);
        cliente.setMotivoBloqueio("Inadimplente");

        assertThatThrownBy(() ->
                service.agendarPeloCliente(cliente, profissional, servico, estabelecimento,
                        LocalDateTime.of(2026, 6, 10, 9, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente bloqueado");
    }

    @Test
    @DisplayName("Deve lançar exceção quando estabelecimento está fechado no dia")
    void agendarPeloCliente_estabelecimentoFechado_lancaExcecao() {
        horario.setAberto(false);
        when(horarioRepository.findByEstabelecimentoIdAndDiaSemana(anyLong(), anyString()))
                .thenReturn(Optional.of(horario));

        assertThatThrownBy(() ->
                service.agendarPeloCliente(cliente, profissional, servico, estabelecimento,
                        LocalDateTime.of(2026, 6, 10, 9, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fechado");
    }

    @Test
    @DisplayName("Deve lançar exceção quando horário está fora do funcionamento")
    void agendarPeloCliente_horarioForaDoFuncionamento_lancaExcecao() {
        when(horarioRepository.findByEstabelecimentoIdAndDiaSemana(anyLong(), anyString()))
                .thenReturn(Optional.of(horario));

        // Tenta agendar às 19h — depois do fechamento às 18h
        assertThatThrownBy(() ->
                service.agendarPeloCliente(cliente, profissional, servico, estabelecimento,
                        LocalDateTime.of(2026, 6, 10, 19, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fora do funcionamento");
    }

    @Test
    @DisplayName("Deve lançar exceção quando há conflito de horário")
    void agendarPeloCliente_conflitoHorario_lancaExcecao() {
        when(horarioRepository.findByEstabelecimentoIdAndDiaSemana(anyLong(), anyString()))
                .thenReturn(Optional.of(horario));

        // Simula agendamento existente no mesmo horário
        Agendamento existente = new Agendamento();
        existente.setStatus(StatusAgendamento.AGENDADO);
        existente.setDataHoraInicio(LocalDateTime.of(2026, 6, 10, 9, 0));
        existente.setDataHoraFim(LocalDateTime.of(2026, 6, 10, 10, 0));
        when(agendamentoRepository.findConflitos(anyLong(), any(), any()))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() ->
                service.agendarPeloCliente(cliente, profissional, servico, estabelecimento,
                        LocalDateTime.of(2026, 6, 10, 9, 0)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ja possui agendamento");
    }

    @Test
    @DisplayName("Deve agendar com sucesso e tentar enviar confirmação")
    void agendarPeloCliente_sucesso_salvaEEnviaConfirmacao() {
        when(horarioRepository.findByEstabelecimentoIdAndDiaSemana(anyLong(), anyString()))
                .thenReturn(Optional.of(horario));
        when(agendamentoRepository.findConflitos(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());

        Agendamento salvo = new Agendamento();
        salvo.setId(10L);
        salvo.setEstabelecimento(estabelecimento);
        when(agendamentoRepository.save(any())).thenReturn(salvo);

        Agendamento resultado = service.agendarPeloCliente(cliente, profissional, servico,
                estabelecimento, LocalDateTime.of(2026, 6, 10, 9, 0));

        assertThat(resultado.getId()).isEqualTo(10L);
        verify(agendamentoRepository).save(any());
        verify(notificacaoService).enviarConfirmacao(salvo);
    }

    @Test
    @DisplayName("Falha no WhatsApp não deve cancelar o agendamento")
    void agendarPeloCliente_falhaWhatsApp_naoLancaExcecao() {
        when(horarioRepository.findByEstabelecimentoIdAndDiaSemana(anyLong(), anyString()))
                .thenReturn(Optional.of(horario));
        when(agendamentoRepository.findConflitos(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());

        Agendamento salvo = new Agendamento();
        salvo.setId(10L);
        salvo.setEstabelecimento(estabelecimento);
        when(agendamentoRepository.save(any())).thenReturn(salvo);
        doThrow(new RuntimeException("WhatsApp offline")).when(notificacaoService)
                .enviarConfirmacao(any());

        // Não deve lançar exceção — WhatsApp é melhor esforço
        assertThatCode(() ->
                service.agendarPeloCliente(cliente, profissional, servico, estabelecimento,
                        LocalDateTime.of(2026, 6, 10, 9, 0)))
                .doesNotThrowAnyException();
    }

    // ── cancelar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve cancelar agendamento com status AGENDADO")
    void cancelar_statusAgendado_cancela() {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setEstabelecimento(estabelecimento);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Agendamento resultado = service.cancelar(1L, 1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    @DisplayName("Não deve cancelar agendamento já concluído")
    void cancelar_statusConcluido_lancaExcecao() {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamento.setEstabelecimento(estabelecimento);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.cancelar(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Apenas agendamentos AGENDADO");
    }

    // ── concluir ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve concluir agendamento com status AGENDADO")
    void concluir_statusAgendado_conclui() {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setEstabelecimento(estabelecimento);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Agendamento resultado = service.concluir(1L, 1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }

    // ── marcarFalta ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve marcar falta e incrementar contador do cliente")
    void marcarFalta_statusAgendado_marcaFaltaEIncrementaContador() {
        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setEstabelecimento(estabelecimento);
        agendamento.setCliente(cliente);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Agendamento resultado = service.marcarFalta(1L, 1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.FALTOU);
        verify(clienteService).registrarFalta(cliente.getId(), 1L);
    }

    // ── buscarPorId ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve lançar exceção quando agendamento pertence a outro estabelecimento")
    void buscarPorId_outroEstabelecimento_lancaExcecao() {
        Estabelecimento outro = new Estabelecimento();
        outro.setId(99L);

        Agendamento agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setEstabelecimento(outro);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.buscarPorId(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nao pertence");
    }

    // campo auxiliar para marcarFalta
    private Agendamento agendamento;
}
