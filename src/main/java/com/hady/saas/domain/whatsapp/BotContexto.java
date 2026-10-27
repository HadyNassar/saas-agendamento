package com.hady.saas.domain.whatsapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexto do fluxo conversacional de agendamento (bot ELITE/PLATINUM).
 *
 * NÃO é uma entidade JPA — é serializado como JSON simples (via ObjectMapper)
 * dentro da coluna TEXT `ConversaWhatsApp.contexto`, que já existia desde a
 * fundação técnica (seção 3 do prompt mestre). Isso evita criar uma tabela nova
 * só para guardar um estado transitório de conversa.
 *
 * Os campos de "IDs ordenados" (servicoIds, profissionalIds, horariosDisponiveis)
 * guardam a MESMA ordem em que foram listados para o cliente, para que a resposta
 * dele (um número) seja mapeada de volta ao item certo — mesmo que a listagem
 * "ao vivo" do banco mude de ordem entre uma mensagem e outra.
 */
public class BotContexto {

    // Passo 1: serviço
    private List<Long> servicoIds = new ArrayList<>();
    private Long servicoId;
    private String servicoNome;
    private Integer servicoDuracaoMinutos;

    // Passo 2: profissional
    private List<Long> profissionalIds = new ArrayList<>();
    private Long profissionalId;
    private String profissionalNome;

    // Passo 3: data
    private String dataEscolhida; // ISO yyyy-MM-dd

    // Passo 4: horário
    private List<String> horariosDisponiveis = new ArrayList<>(); // "HH:mm", na ordem exibida
    private String horarioEscolhido; // "HH:mm"

    // Item 2: usado só quando ConversaWhatsApp.estado == AGUARDANDO_CONFIRMACAO_LEMBRETE.
    // Identifica qual agendamento o cliente está confirmando/cancelando pela resposta.
    private Long agendamentoId;

    // Item 3 (reagendamento self-service):
    // Passo A — se o cliente tem mais de um agendamento futuro ativo, guarda os
    // IDs na ordem exibida (mesmo padrão de servicoIds/profissionalIds acima),
    // pra mapear a resposta numérica dele de volta ao agendamento certo.
    private List<Long> agendamentosParaEscolherIds = new ArrayList<>();
    // Passo B — depois de escolhido (ou já sabido, se só havia um), este é o
    // agendamento que está sendo reagendado. Reaproveita o profissional e a
    // duração do serviço originais para gerar os novos slots (mesmo profissional).
    private Long agendamentoOriginalId;
    private Long reagendamentoProfissionalId;
    private Integer reagendamentoServicoDuracaoMinutos;

    // Item 4 (menu principal): quando o cliente escolhe "3-Cancelar" no menu e
    // tem mais de um agendamento futuro ativo, guarda os IDs na ordem exibida
    // (mesmo padrão de agendamentosParaEscolherIds do reagendamento), pra
    // mapear a resposta numérica de volta ao agendamento certo a cancelar.
    // Campo separado do de reagendamento para não misturar os dois fluxos.
    private List<Long> agendamentosParaCancelarIds = new ArrayList<>();

    // Opções oferecidas quando o dia pedido está sem vaga (ver
    // BotAgendamentoService.oferecerAlternativas): mesma profissional em outro
    // dia e/ou outras profissionais do mesmo serviço com vaga antes.
    private List<Alternativa> alternativas = new ArrayList<>();

    public BotContexto() {}

    /** Uma sugestão de "profissional + primeiro dia com vaga". Serializada em JSON junto do contexto. */
    public static class Alternativa {
        private Long profissionalId;
        private String profissionalNome;
        private String data; // ISO yyyy-MM-dd

        public Alternativa() {}
        public Alternativa(Long profissionalId, String profissionalNome, String data) {
            this.profissionalId = profissionalId;
            this.profissionalNome = profissionalNome;
            this.data = data;
        }
        public Long getProfissionalId() { return profissionalId; }
        public String getProfissionalNome() { return profissionalNome; }
        public String getData() { return data; }
        public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
        public void setProfissionalNome(String profissionalNome) { this.profissionalNome = profissionalNome; }
        public void setData(String data) { this.data = data; }
    }

    // GETTERS
    public List<Long> getServicoIds() { return servicoIds; }
    public Long getServicoId() { return servicoId; }
    public String getServicoNome() { return servicoNome; }
    public Integer getServicoDuracaoMinutos() { return servicoDuracaoMinutos; }
    public List<Long> getProfissionalIds() { return profissionalIds; }
    public Long getProfissionalId() { return profissionalId; }
    public String getProfissionalNome() { return profissionalNome; }
    public String getDataEscolhida() { return dataEscolhida; }
    public List<String> getHorariosDisponiveis() { return horariosDisponiveis; }
    public String getHorarioEscolhido() { return horarioEscolhido; }
    public Long getAgendamentoId() { return agendamentoId; }
    public List<Long> getAgendamentosParaEscolherIds() { return agendamentosParaEscolherIds; }
    public Long getAgendamentoOriginalId() { return agendamentoOriginalId; }
    public Long getReagendamentoProfissionalId() { return reagendamentoProfissionalId; }
    public Integer getReagendamentoServicoDuracaoMinutos() { return reagendamentoServicoDuracaoMinutos; }
    public List<Long> getAgendamentosParaCancelarIds() { return agendamentosParaCancelarIds; }
    public List<Alternativa> getAlternativas() { return alternativas; }

    // SETTERS
    public void setServicoIds(List<Long> servicoIds) { this.servicoIds = servicoIds; }
    public void setServicoId(Long servicoId) { this.servicoId = servicoId; }
    public void setServicoNome(String servicoNome) { this.servicoNome = servicoNome; }
    public void setServicoDuracaoMinutos(Integer servicoDuracaoMinutos) { this.servicoDuracaoMinutos = servicoDuracaoMinutos; }
    public void setProfissionalIds(List<Long> profissionalIds) { this.profissionalIds = profissionalIds; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public void setProfissionalNome(String profissionalNome) { this.profissionalNome = profissionalNome; }
    public void setDataEscolhida(String dataEscolhida) { this.dataEscolhida = dataEscolhida; }
    public void setHorariosDisponiveis(List<String> horariosDisponiveis) { this.horariosDisponiveis = horariosDisponiveis; }
    public void setHorarioEscolhido(String horarioEscolhido) { this.horarioEscolhido = horarioEscolhido; }
    public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
    public void setAgendamentosParaEscolherIds(List<Long> agendamentosParaEscolherIds) { this.agendamentosParaEscolherIds = agendamentosParaEscolherIds; }
    public void setAgendamentoOriginalId(Long agendamentoOriginalId) { this.agendamentoOriginalId = agendamentoOriginalId; }
    public void setReagendamentoProfissionalId(Long reagendamentoProfissionalId) { this.reagendamentoProfissionalId = reagendamentoProfissionalId; }
    public void setReagendamentoServicoDuracaoMinutos(Integer reagendamentoServicoDuracaoMinutos) { this.reagendamentoServicoDuracaoMinutos = reagendamentoServicoDuracaoMinutos; }
    public void setAgendamentosParaCancelarIds(List<Long> agendamentosParaCancelarIds) { this.agendamentosParaCancelarIds = agendamentosParaCancelarIds; }
    public void setAlternativas(List<Alternativa> alternativas) { this.alternativas = alternativas; }
}