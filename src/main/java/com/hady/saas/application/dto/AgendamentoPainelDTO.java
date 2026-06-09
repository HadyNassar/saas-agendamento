package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// DTO para criar agendamento pelo PAINEL INTERNO
public class AgendamentoPainelDTO {

    @NotNull(message = "Cliente obrigatório")
    private Long clienteId;

    @NotNull(message = "Profissional obrigatório")
    private Long profissionalId;

    @NotNull(message = "Serviço obrigatório")
    private Long servicoId;

    @NotNull(message = "Data e hora obrigatórias")
    private LocalDateTime dataHoraInicio;

    private String observacoes;

    // GETTERS
    public Long getClienteId() { return clienteId; }
    public Long getProfissionalId() { return profissionalId; }
    public Long getServicoId() { return servicoId; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public String getObservacoes() { return observacoes; }

    // SETTERS
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public void setServicoId(Long servicoId) { this.servicoId = servicoId; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
