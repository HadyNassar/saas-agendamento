package com.hady.saas.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// DTO para criar agendamento pelo LINK PÚBLICO (sem login)
public class AgendamentoPublicoDTO {

    // Dados do cliente (cadastro automático pelo telefone)
    @NotBlank(message = "Nome obrigatório")
    private String clienteNome;

    @NotBlank(message = "Telefone obrigatório")
    private String clienteTelefone;

    private String clienteEmail;

    @NotNull(message = "Profissional obrigatório")
    private Long profissionalId;

    @NotNull(message = "Serviço obrigatório")
    private Long servicoId;

    @NotNull(message = "Data e hora obrigatórias")
    private LocalDateTime dataHoraInicio;

    private boolean aguardandoPagamento;

    // GETTERS
    public String getClienteNome() { return clienteNome; }
    public String getClienteTelefone() { return clienteTelefone; }
    public String getClienteEmail() { return clienteEmail; }
    public Long getProfissionalId() { return profissionalId; }
    public Long getServicoId() { return servicoId; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public boolean isAguardandoPagamento() { return aguardandoPagamento; }

    // SETTERS
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public void setClienteTelefone(String clienteTelefone) { this.clienteTelefone = clienteTelefone; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public void setServicoId(Long servicoId) { this.servicoId = servicoId; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public void setAguardandoPagamento(boolean aguardandoPagamento) { this.aguardandoPagamento = aguardandoPagamento; }
}
