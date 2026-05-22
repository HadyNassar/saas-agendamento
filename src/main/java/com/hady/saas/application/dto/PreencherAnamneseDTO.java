package com.hady.saas.application.dto;

import java.util.Map;

// DTO para enviar respostas da anamnese
// Key do map = ID da pergunta
// Value = resposta + detalhe
public class PreencherAnamneseDTO {

    // Map<perguntaId, RespostaItemDTO>
    private Map<Long, RespostaItemDTO> respostas;

    public Map<Long, RespostaItemDTO> getRespostas() { return respostas; }
    public void setRespostas(Map<Long, RespostaItemDTO> respostas) { this.respostas = respostas; }

    public static class RespostaItemDTO {
        private String resposta; // "SIM", "NAO", ou texto livre
        private String detalhe;  // preenchido quando SIM_NAO_DETALHE e resposta = SIM

        public String getResposta() { return resposta; }
        public String getDetalhe() { return detalhe; }
        public void setResposta(String resposta) { this.resposta = resposta; }
        public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    }
}
