package com.hady.saas.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ficha completa do cliente — usada tanto no painel quanto na página pública.
 * Inclui dados cadastrais, histórico de serviços, anamnese e fotos antes/depois.
 */
public class FichaClienteDTO {

    // Dados cadastrais
    private Long id;
    private String nome;
    private String telefone;
    private String email;
    private String alergias;
    private String preferencias;
    private String restricoes;
    private String observacoes;
    private Boolean bloqueado;
    private Integer totalFaltas;
    private LocalDateTime criadoEm;

    // Histórico de agendamentos (últimos 20)
    private List<AgendamentoResumoDTO> historico;

    // Respostas de anamnese
    private List<RespostaAnamneseDTO> anamnese;

    // Fotos antes/depois
    private List<FotoClienteDTO> fotos;

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getAlergias() { return alergias; }
    public String getPreferencias() { return preferencias; }
    public String getRestricoes() { return restricoes; }
    public String getObservacoes() { return observacoes; }
    public Boolean getBloqueado() { return bloqueado; }
    public Integer getTotalFaltas() { return totalFaltas; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<AgendamentoResumoDTO> getHistorico() { return historico; }
    public List<RespostaAnamneseDTO> getAnamnese() { return anamnese; }
    public List<FotoClienteDTO> getFotos() { return fotos; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEmail(String email) { this.email = email; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
    public void setPreferencias(String preferencias) { this.preferencias = preferencias; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
    public void setTotalFaltas(Integer totalFaltas) { this.totalFaltas = totalFaltas; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public void setHistorico(List<AgendamentoResumoDTO> historico) { this.historico = historico; }
    public void setAnamnese(List<RespostaAnamneseDTO> anamnese) { this.anamnese = anamnese; }
    public void setFotos(List<FotoClienteDTO> fotos) { this.fotos = fotos; }

    // ── Inner DTOs ─────────────────────────────────────────────────────────────

    public static class AgendamentoResumoDTO {
        private Long id;
        private LocalDateTime dataHoraInicio;
        private String status;
        private String profissionalNome;
        private String servicoNome;
        private Double valor;

        public Long getId() { return id; }
        public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
        public String getStatus() { return status; }
        public String getProfissionalNome() { return profissionalNome; }
        public String getServicoNome() { return servicoNome; }
        public Double getValor() { return valor; }

        public void setId(Long id) { this.id = id; }
        public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
        public void setStatus(String status) { this.status = status; }
        public void setProfissionalNome(String profissionalNome) { this.profissionalNome = profissionalNome; }
        public void setServicoNome(String servicoNome) { this.servicoNome = servicoNome; }
        public void setValor(Double valor) { this.valor = valor; }
    }

    public static class RespostaAnamneseDTO {
        private Long perguntaId;
        private String pergunta;
        private String resposta;
        private String detalhe;

        public Long getPerguntaId() { return perguntaId; }
        public String getPergunta() { return pergunta; }
        public String getResposta() { return resposta; }
        public String getDetalhe() { return detalhe; }

        public void setPerguntaId(Long perguntaId) { this.perguntaId = perguntaId; }
        public void setPergunta(String pergunta) { this.pergunta = pergunta; }
        public void setResposta(String resposta) { this.resposta = resposta; }
        public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    }
}
