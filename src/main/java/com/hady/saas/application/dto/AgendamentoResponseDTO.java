package com.hady.saas.application.dto;

import com.hady.saas.domain.agendamento.Agendamento;

import java.time.LocalDateTime;

public class AgendamentoResponseDTO {

    private Long id;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String status;

    private Long clienteId;
    private String clienteNome;
    private String clienteTelefone;

    private Long profissionalId;
    private String profissionalNome;

    private Long servicoId;
    private String servicoNome;

    private String observacoes;
    private boolean agendadoPeloCliente;

    public AgendamentoResponseDTO() {}

    // ── construtor a partir da entidade ──────────────────────────────────────
    public static AgendamentoResponseDTO de(Agendamento a) {
        AgendamentoResponseDTO dto = new AgendamentoResponseDTO();
        dto.id                  = a.getId();
        dto.dataHoraInicio      = a.getDataHoraInicio();
        dto.dataHoraFim         = a.getDataHoraFim();
        dto.status              = a.getStatus().name();
        dto.observacoes         = a.getObservacoes();
        dto.agendadoPeloCliente = a.isAgendadoPeloCliente();

        if (a.getCliente() != null) {
            dto.clienteId       = a.getCliente().getId();
            dto.clienteNome     = a.getCliente().getNome();
            dto.clienteTelefone = a.getCliente().getTelefone();
        }
        if (a.getProfissional() != null) {
            dto.profissionalId   = a.getProfissional().getId();
            dto.profissionalNome = a.getProfissional().getNome();
        }
        if (a.getServico() != null) {
            dto.servicoId   = a.getServico().getId();
            dto.servicoNome = a.getServico().getNome();
        }
        return dto;
    }

    // ── getters ───────────────────────────────────────────────────────────────
    public Long getId()                    { return id; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public LocalDateTime getDataHoraFim()    { return dataHoraFim; }
    public String getStatus()              { return status; }
    public Long getClienteId()             { return clienteId; }
    public String getClienteNome()         { return clienteNome; }
    public String getClienteTelefone()     { return clienteTelefone; }
    public Long getProfissionalId()        { return profissionalId; }
    public String getProfissionalNome()    { return profissionalNome; }
    public Long getServicoId()             { return servicoId; }
    public String getServicoNome()         { return servicoNome; }
    public String getObservacoes()         { return observacoes; }
    public boolean isAgendadoPeloCliente() { return agendadoPeloCliente; }
}
