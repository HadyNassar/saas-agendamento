package com.hady.saas.application.dto;

// DTO para POST /app/{slug}/pagamento — cliente paga o agendamento
public class PagamentoAgendamentoDTO {

    private Long   agendamentoId;
    private String metodoPagamento;  // "pix" | "credit_card" | "debit_card"
    private String cardToken;        // obrigatório para cartão, null para pix
    private String emailPagador;
    private Integer parcelas;        // default 1

    public Long getAgendamentoId()           { return agendamentoId; }
    public void setAgendamentoId(Long v)     { this.agendamentoId = v; }

    public String getMetodoPagamento()           { return metodoPagamento; }
    public void setMetodoPagamento(String v)     { this.metodoPagamento = v; }

    public String getCardToken()           { return cardToken; }
    public void setCardToken(String v)     { this.cardToken = v; }

    public String getEmailPagador()           { return emailPagador; }
    public void setEmailPagador(String v)     { this.emailPagador = v; }

    public Integer getParcelas()               { return parcelas != null ? parcelas : 1; }
    public void setParcelas(Integer v)         { this.parcelas = v; }
}
